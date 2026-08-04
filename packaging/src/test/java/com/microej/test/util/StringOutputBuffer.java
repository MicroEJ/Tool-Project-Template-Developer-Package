/*
 * Java
 *
 * Copyright 2025-2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

package com.microej.test.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides access to a {@link java.io.Writer} that can be used to record a text output, and convenience methods to
 * inspect the written content.
 */
public class StringOutputBuffer implements AutoCloseable {

	static final String PROPERTY_CHECK_TIMEOUT = "string.output.wait.timeout";
	private static final int DEFAULT_CHECK_TIMEOUT_MILLIS = 60_000;

	private static final Logger LOGGER = Logger.getLogger(StringOutputBuffer.class.getName());

	private final StringOutputWriter writer;
	private final Lock bufferLock = new ReentrantLock();
	private final Condition newData = bufferLock.newCondition();

	public StringOutputBuffer() {
		this.writer = new StringOutputWriter();
	}

	/**
	 * Returns the {@link java.io.Writer} to write text content into.
	 *
	 * @return the writer to write to
	 */
	public Writer getWriter() {
		return this.writer;
	}

	/**
	 * Returns the text content written to the {@link java.io.Writer}.
	 *
	 * @return the text written to the {@link java.io.Writer}
	 */
	public String getContent() {
		this.bufferLock.lock();
		try {
			return this.writer.toString();
		} finally {
			this.bufferLock.unlock();
		}
	}

	/**
	 * Waits until the written content contains the specified text.
	 *
	 * <p>
	 * This method blocks the calling thread until either the specified text is detected in the output or the timeout
	 * period elapses.
	 *
	 * @param expected
	 *            the text to check in the written content
	 * @param timeoutMillis
	 *            the timeout period to wait before the check is considered unsuccessful
	 * @return {@code true} if the specified text is detected in the written content before the timeout elapses,
	 *         {@code false} otherwise
	 * @see #waitUntilContains(String)
	 */
	public boolean waitUntilContains(String expected, int timeoutMillis) {
		if (timeoutMillis < 0) {
			throw new IllegalArgumentException(
					"Invalid value for timeout: " + timeoutMillis + ". Expected a value greater or equal to 0.");
		}

		long timeoutTime = System.currentTimeMillis() + timeoutMillis;

		this.bufferLock.lock();
		try {
			while (this.writer.isOpen() && System.currentTimeMillis() < timeoutTime) {
				if (getContent().contains(expected)) {
					return true;
				}

				long remainingTime = timeoutTime - System.currentTimeMillis();
				if (remainingTime > 0) {
					try {
						this.newData.await(remainingTime, TimeUnit.MILLISECONDS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						LOGGER.log(Level.WARNING, "Interrupted while waiting for new text input.", e);
						return false;
					}
				}
			}
			if (System.currentTimeMillis() >= timeoutTime) {
				LOGGER.warning("Waiting time exceeded timeout limit (" + timeoutMillis + " ms).");
			}
			return false;
		} finally {
			this.bufferLock.unlock();
		}
	}

	/**
	 * Waits until the written content contains the specified text.
	 *
	 * <p>
	 * This method blocks the calling thread until either the specified text is detected in the output or the timeout
	 * period elapses. This method uses the timeout delay set with the system property {@link #PROPERTY_CHECK_TIMEOUT} .
	 *
	 * @param expected
	 *            the text to check in the written content
	 * @return {@code true} if the specified text is detected in the written content before the timeout elapses,
	 *         {@code false} otherwise
	 * @see #waitUntilContains(String, int)
	 */
	public boolean waitUntilContains(String expected) {
		return waitUntilContains(expected, Integer.getInteger(PROPERTY_CHECK_TIMEOUT, DEFAULT_CHECK_TIMEOUT_MILLIS));
	}

	@Override
	public void close() throws IOException {
		this.writer.close();
	}

	private class StringOutputWriter extends Writer {

		private final StringWriter writer;
		private final PrintWriter out;
		private volatile boolean closed;

		StringOutputWriter() {
			this.writer = new StringWriter();
			this.out = new PrintWriter(System.out);
			this.closed = false;
		}

		@Override
		public void write(char[] cbuf, int off, int len) {
			try {
				bufferLock.lock();
				if (this.closed) {
					return;
				}
				this.writer.write(cbuf, off, len);
				this.out.write(cbuf, off, len);
			} finally {
				bufferLock.unlock();
			}
		}

		@Override
		public void flush() {
			bufferLock.lock();
			try {
				if (this.closed) {
					return;
				}
				this.writer.flush();
				this.out.flush();
				newData.signalAll();
			} finally {
				bufferLock.unlock();
			}
		}

		@Override
		public void close() {
			try {
				bufferLock.lock();
				this.closed = true;
				this.writer.close();
				this.out.close();
				newData.signalAll();
			} catch (IOException e) {
				// can't happen with StringWriter or PrintWriter
			} finally {
				bufferLock.unlock();
			}
		}

		@Override
		public String toString() {
			return this.writer.toString();
		}

		private boolean isOpen() {
			return !this.closed;
		}
	}
}
