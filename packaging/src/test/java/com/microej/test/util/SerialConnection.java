/*
 * Java
 *
 * Copyright 2025-2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

package com.microej.test.util;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

/**
 * Manages a serial port connection, and allows text detection on received serial data.
 * <p>
 * The serial connection uses the specified configuration parameters, including port name, baud rate, data bits, stop
 * bits, and parity.
 * <p>
 * The received data is stored internally in a buffer. The buffer's content can be checked for the presence of a
 * specific text by calling {@link #waitUntilContains(String)}.
 */
public class SerialConnection extends StringOutputBuffer {

	static final String PROPERTY_PORT_NAME = "serial.connection.port";
	static final String PROPERTY_BAUDRATE = "serial.connection.baudrate";
	static final String PROPERTY_DATA_BITS = "serial.connection.databits";
	static final String PROPERTY_STOP_BITS = "serial.connection.stopbits";
	static final String PROPERTY_PARITY = "serial.connection.parity";
	static final String PROPERTY_MAX_RETRIES = "serial.connection.retry";

	private static final int DEFAULT_BAUDRATE = 115200;
	private static final int DEFAULT_DATA_BITS = 8;
	private static final int DEFAULT_STOP_BITS = SerialPort.ONE_STOP_BIT;
	private static final String DEFAULT_PARITY = "none";
	public static final int RETRY_DELAY_MILLIS = 5_000;
	public static final int DEFAULT_MAX_RETRIES = 0;

	private final String portName;
	private final int baudRate;
	private final int databits;
	private final int stopbits;
	private final int parity;
	private SerialPort port;
	private ScheduledExecutorService scheduler;
	private volatile boolean retrying;
	private int retries;
	private final int maxRetries;

	/**
	 * Creates a {@code SerialConnection} and tries to open a serial port with the specified configuration.
	 * <p>
	 * After the serial port is opened, any unexpected disconnection (e.g., if the device is unplugged) triggers
	 * automatic reconnection attempts. The number of retry attempts is configurable via the system property
	 * {@link #PROPERTY_MAX_RETRIES} (defaults to {@link #DEFAULT_MAX_RETRIES}).
	 *
	 * @param portName
	 *            the name of the serial port (e.g., "COM3" or "/dev/ttyUSB0"), must not be null or empty
	 * @param baudRate
	 *            the baud rate for the connection (e.g., 9600, 115200)
	 * @param databits
	 *            the number of data bits, allowed values are 5, 6, 7, or 8
	 * @param stopbits
	 *            the number of stop bits, allowed values are 1 or 2
	 * @param parity
	 *            the parity, allowed values are "none", "even" or "odd"
	 * @param maxRetries
	 *            the number of retry attempts to perform if the port is unexpectedly closed, must be greater or equal
	 *            to 0
	 * @throws java.io.IOException
	 *             if the serial port could not be opened
	 */
	public SerialConnection(String portName, int baudRate, int databits, int stopbits, String parity, int maxRetries)
			throws IOException {
		if (portName == null || portName.isBlank()) {
			throw new IllegalArgumentException("Port name cannot be null or empty.");
		}
		if (databits < 5 || databits > 8) {
			throw new IllegalArgumentException(
					"Invalid value for data bits: " + databits + ". Expected one of {5, 6, 7, 8}.");
		}
		if (maxRetries < 0) {
			throw new IllegalArgumentException(
					"Invalid value for max retries: " + maxRetries + ". Value must be greater or equal to 0.");
		}
		this.portName = portName;
		this.baudRate = baudRate;
		this.databits = databits;
		this.stopbits = parseStopBits(stopbits);
		this.parity = parseParity(parity);
		this.maxRetries = maxRetries;
		openPort();
	}

	/**
	 * Creates a {@code SerialConnection} from the serial port configuration specified as System Properties.
	 * <p>
	 * This method sets up the serial port parameters but does not open the connection. To open the connection, call
	 * {@link #openPort()} after instantiation.
	 *
	 * @see #PROPERTY_PORT_NAME
	 * @see #PROPERTY_BAUDRATE
	 * @see #PROPERTY_DATA_BITS
	 * @see #PROPERTY_STOP_BITS
	 * @see #PROPERTY_PARITY
	 * @see #PROPERTY_MAX_RETRIES
	 */
	public static SerialConnection fromSystemProperties() throws IOException {
		String portName = System.getProperty(PROPERTY_PORT_NAME, "");
		int baudRate = Integer.getInteger(PROPERTY_BAUDRATE, DEFAULT_BAUDRATE);
		int dataBits = Integer.getInteger(PROPERTY_DATA_BITS, DEFAULT_DATA_BITS);
		int stopbits = Integer.getInteger(PROPERTY_STOP_BITS, DEFAULT_STOP_BITS);
		int maxRetries = Integer.getInteger(PROPERTY_MAX_RETRIES, DEFAULT_MAX_RETRIES);
		String parity = System.getProperty(PROPERTY_PARITY, DEFAULT_PARITY);
		return new SerialConnection(portName, baudRate, dataBits, stopbits, parity, maxRetries);
	}

	@Override
	public void close() throws IOException {
		closePort();
		super.close();
	}

	private void openPort() throws IOException {
		SerialPort[] commPorts = SerialPort.getCommPorts();
		for (SerialPort serialPort : commPorts) {
			if (serialPort.getSystemPortName().equals(this.portName)) {
				SerialPort newPort = SerialPort.getCommPort(this.portName);
				newPort.flushIOBuffers();
				newPort.setComPortParameters(this.baudRate, this.databits, this.stopbits, this.parity);

				if (newPort.isOpen()) {
					return;
				}

				if (newPort.openPort()) {
					newPort.addDataListener(new SerialPortDataListener() {

						private final StringBuilder buffer = new StringBuilder();

						@Override
						public int getListeningEvents() {
							return SerialPort.LISTENING_EVENT_DATA_AVAILABLE
									| SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
						}

						@Override
						public void serialEvent(SerialPortEvent event) {
							if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
								byte[] data = new byte[newPort.bytesAvailable()];
								newPort.readBytes(data, data.length);
								write(data);
							} else if (event.getEventType() == SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) {
								closePort();
								retryOpenPort();
							}
						}

						private void write(byte[] data) {
							for (byte b : data) {
								char c = (char) b;
								if (c == '\n') {
									String line = this.buffer.toString().trim();
									Writer writer = getWriter();
									try {
										writer.write(line);
										writer.write(System.lineSeparator());
										writer.flush();
									} catch (IOException e) {
										// Ignore the exception, the writer might be closed now
									}
									this.buffer.setLength(0);
								} else {
									this.buffer.append(c);
								}
							}
						}
					});
					this.port = newPort;
					return;
				}
			}
		}
		throw new IOException("Could not open serial port " + this.portName);
	}

	private void closePort() {
		if (this.port != null) {
			this.port.closePort();
			this.port.removeDataListener();
		}
		if (this.scheduler != null) {
			this.scheduler.shutdownNow();
		}
	}

	private void retryOpenPort() {
		if (this.retrying || this.maxRetries < 1) {
			return;
		}
		this.retrying = true;
		this.retries = 0;
		this.scheduler = Executors.newSingleThreadScheduledExecutor();
		this.scheduler.scheduleWithFixedDelay(() -> {
			if (this.retries < this.maxRetries) {
				try {
					openPort();
				} catch (IOException e) {
					this.retries++;
					return;
				}
			}
			this.retrying = false;
			this.scheduler.shutdownNow();
		}, 0, RETRY_DELAY_MILLIS, TimeUnit.MILLISECONDS);
	}

	private static int parseParity(String parity) {
		if (parity == null || parity.isBlank()) {
			throw new IllegalArgumentException("Invalid value for parity. Expected one of {none, odd, even}.");
		}

		switch (parity.toLowerCase()) {
		case "none":
			return SerialPort.NO_PARITY;
		case "odd":
			return SerialPort.ODD_PARITY;
		case "even":
			return SerialPort.EVEN_PARITY;
		default:
			throw new IllegalArgumentException(
					"Invalid value for parity: " + parity + ". Expected one of {none, odd, even}.");
		}
	}

	private static int parseStopBits(int stopbits) {
		if (stopbits == 1) {
			return SerialPort.ONE_STOP_BIT;
		} else if (stopbits == 2) {
			return SerialPort.TWO_STOP_BITS;
		} else {
			throw new IllegalArgumentException("Invalid value for stopbits: " + stopbits + ". Expected one of {1, 2}.");
		}
	}
}
