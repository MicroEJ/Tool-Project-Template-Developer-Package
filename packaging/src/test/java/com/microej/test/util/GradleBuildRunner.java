/*
 * Java
 *
 * Copyright 2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

package com.microej.test.util;

import static com.microej.test.util.SerialConnection.PROPERTY_BAUDRATE;
import static com.microej.test.util.SerialConnection.PROPERTY_DATA_BITS;
import static com.microej.test.util.SerialConnection.PROPERTY_MAX_RETRIES;
import static com.microej.test.util.SerialConnection.PROPERTY_PARITY;
import static com.microej.test.util.SerialConnection.PROPERTY_PORT_NAME;
import static com.microej.test.util.SerialConnection.PROPERTY_STOP_BITS;
import static com.microej.test.util.StringOutputBuffer.PROPERTY_CHECK_TIMEOUT;
import static com.microej.test.util.TestProperties.DEPLOY_NAME;
import static com.microej.test.util.TestProperties.IS_WINDOWS;
import static com.microej.test.util.TestProperties.JENKINS_JOB_NAME;
import static com.microej.test.util.TestProperties.JENKINS_JOB_PREFIX;
import static com.microej.test.util.TestProperties.JENKINS_TRACE_FILE;
import static com.microej.test.util.TestProperties.JENKINS_URL;
import static com.microej.test.util.TestProperties.JENKINS_USER_NAME;
import static com.microej.test.util.TestProperties.JENKINS_USER_TOKEN;
import static com.microej.test.util.TestProperties.PROPERTY_DEPLOY_NAME;
import static com.microej.test.util.TestProperties.PROPERTY_JENKINS_JOB_NAME;
import static com.microej.test.util.TestProperties.PROPERTY_JENKINS_JOB_PREFIX;
import static com.microej.test.util.TestProperties.PROPERTY_JENKINS_TRACE_FILE;
import static com.microej.test.util.TestProperties.PROPERTY_JENKINS_URL;
import static com.microej.test.util.TestProperties.PROPERTY_JENKINS_USER_NAME;
import static com.microej.test.util.TestProperties.PROPERTY_JENKINS_USER_TOKEN;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs Gradle builds (synchronous and asynchronous) and manages the lifecycle of the started processes.
 *
 * <p>
 * Asynchronous processes are tracked internally and can be destroyed by calling {@link #destroyAllProcesses()}.
 */
public class GradleBuildRunner {

	/**
	 * Return code on build success.
	 */
	public static final int BUILD_SUCCESS = 0;

	/**
	 * Return code on build failure.
	 */
	public static final int BUILD_FAILURE = 1;

	private static final Logger LOGGER = Logger.getLogger(GradleBuildRunner.class.getName());

	private final List<Process> processes = new ArrayList<>();

	/**
	 * Starts a new Gradle build, given the root directory of a project and a list of build arguments.
	 *
	 * <p>
	 * The root project directory must contain a Gradle Wrapper executable {@code gradlew}.
	 *
	 * <p>
	 * The writer is only written to, it is never closed: the creator of the writer keeps its ownership.
	 *
	 * @param projectDir
	 *            the root project of a Gradle project
	 * @param writer
	 *            a writer to write standard and error to
	 * @param args
	 *            a list of arguments for the build
	 * @return {@link #BUILD_SUCCESS} if the build is successful, {@link #BUILD_FAILURE} otherwise
	 */
	public int startBuild(File projectDir, Writer writer, String... args) {
		try {
			Process process = startGradleProcess(projectDir, writer, getBuildArguments(args));
			return process.waitFor();
		} catch (IOException | InterruptedException e) {
			LOGGER.log(Level.SEVERE, "An error occurred when starting a Gradle build.", e);
			return BUILD_FAILURE;
		}
	}

	/**
	 * Starts a new Gradle build, given the root directory of a project and a list of build arguments.
	 *
	 * <p>
	 * The root project directory must contain a Gradle Wrapper executable {@code gradlew}.
	 *
	 * <p>
	 * The output of the build is redirected to the standard output (including error stream).
	 *
	 * @param projectDir
	 *            the root project of a Gradle project
	 * @param args
	 *            a list of arguments for the build
	 * @return {@link #BUILD_SUCCESS} if the build is successful, {@link #BUILD_FAILURE} otherwise
	 */
	public int startBuild(File projectDir, String... args) {
		return startBuild(projectDir, new OutputStreamWriter(System.out), args);
	}

	/**
	 * Starts a new Gradle build asynchronously, given the root directory of a project and a list of build arguments.
	 *
	 * <p>
	 * The root project directory must contain a Gradle Wrapper executable {@code gradlew}.
	 *
	 * <p>
	 * Use this method for a build that does not terminate on its own, typically a {@code runOnDevice} task that stays
	 * attached to the running executable. For a build that terminates once its work is done, for example a
	 * {@code runOnDevice} task that returns as soon as the executable is deployed, use
	 * {@link #startBuild(File, Writer, String...)} instead.
	 *
	 * <p>
	 * The writer is only written to, it is never closed: the creator of the writer keeps its ownership. The writer
	 * therefore stays usable after the exit of the process, for a {@link SerialConnection} or a later build that shares
	 * it.
	 *
	 * <p>
	 * The started process is tracked internally and can be destroyed individually by calling
	 * {@link #destroyProcess(Process)} or together with all other tracked processes by calling
	 * {@link #destroyAllProcesses()}.
	 *
	 * @param projectDir
	 *            the root project of a Gradle project
	 * @param writer
	 *            a writer to write standard and error to
	 * @param args
	 *            a list of arguments for the build
	 * @return the started process, so the caller can destroy it as soon as it is no longer needed
	 * @throws IOException
	 *             if an I/O error occurs when starting the Gradle process, or if the project does not contain the
	 *             Gradle Wrapper
	 */
	public Process startAsynchronousBuild(File projectDir, Writer writer, String... args) throws IOException {
		Process process = startGradleProcess(projectDir, writer, getBuildArguments(args));
		processes.add(process);
		return process;
	}

	/**
	 * Destroys the given process (and its descendants) and removes it from the tracked process list.
	 *
	 * <p>
	 * This method blocks until the process has actually terminated, so the OS has reaped it before this method returns.
	 *
	 * @param process
	 *            the process to destroy, previously returned by {@link #startAsynchronousBuild}
	 */
	public void destroyProcess(Process process) {
		process.descendants().forEach(ProcessHandle::destroyForcibly);
		process.destroyForcibly();
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		processes.remove(process);
	}

	/**
	 * Destroys all processes started by {@link #startAsynchronousBuild} and clears the tracked process list.
	 *
	 * <p>
	 * This method blocks until each process has actually terminated, so the OS has reaped them before this method
	 * returns.
	 */
	public void destroyAllProcesses() {
		for (Process process : processes) {
			process.descendants().forEach(ProcessHandle::destroyForcibly);
			process.destroyForcibly();
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		processes.clear();
	}

	/**
	 * Returns the full list of arguments required for a build command, given the specified list of arguments.
	 */
	private String[] getBuildArguments(String... args) {
		List<String> arguments = new ArrayList<>();

		File testPackageDir = TestUtil.getTestedPackageDir();
		arguments.add("-g");
		arguments.add(testPackageDir.toPath().resolveSibling("gradle-home").toString());
		arguments.add("-Daccept-microej-sdk-eula-v3-1c=YES");
		if ("jenkins".equals(DEPLOY_NAME)) {
			arguments.add("-D" + PROPERTY_DEPLOY_NAME + "=" + DEPLOY_NAME);
			addMicroejOptionIfPresent(arguments, PROPERTY_JENKINS_TRACE_FILE, JENKINS_TRACE_FILE);
			addMicroejOptionIfPresent(arguments, PROPERTY_JENKINS_URL, JENKINS_URL);
			addMicroejOptionIfPresent(arguments, PROPERTY_JENKINS_JOB_PREFIX, JENKINS_JOB_PREFIX);
			addMicroejOptionIfPresent(arguments, PROPERTY_JENKINS_JOB_NAME, JENKINS_JOB_NAME);
			addMicroejOptionIfPresent(arguments, PROPERTY_JENKINS_USER_NAME, JENKINS_USER_NAME);
			addMicroejOptionIfPresent(arguments, PROPERTY_JENKINS_USER_TOKEN, JENKINS_USER_TOKEN);
		} else {
			addSystemPropertyIfPresent(arguments, PROPERTY_PORT_NAME);
			addSystemPropertyIfPresent(arguments, PROPERTY_BAUDRATE);
			addSystemPropertyIfPresent(arguments, PROPERTY_DATA_BITS);
			addSystemPropertyIfPresent(arguments, PROPERTY_STOP_BITS);
			addSystemPropertyIfPresent(arguments, PROPERTY_PARITY);
			addSystemPropertyIfPresent(arguments, PROPERTY_MAX_RETRIES);
			addSystemPropertyIfPresent(arguments, PROPERTY_CHECK_TIMEOUT);
		}
		// User-supplied args are added last so they can override any injected -D flag (Gradle "later wins" semantics).
		arguments.addAll(List.of(args));
		return arguments.toArray(String[]::new);
	}

	private static void addSystemPropertyIfPresent(List<String> arguments, String property) {
		String value = System.getProperty(property);
		if (value != null) {
			arguments.add("-D" + property + "=" + value);
		}
	}

	private static void addMicroejOptionIfPresent(List<String> arguments, String key, String value) {
		if (value != null && !value.isEmpty()) {
			arguments.add("-Dmicroej.option." + key + "=" + value);
		}
	}

	private static Process startGradleProcess(File projectDir, final Writer writer, String... args) throws IOException {
		File gradlewExecutable = new File(projectDir, "gradlew");
		if (!gradlewExecutable.exists()) {
			throw new IOException("Gradle Wrapper not found in project " + projectDir
					+ ". Please specify a project that includes a Gradle Wrapper.");
		}

		List<String> command = new ArrayList<>();

		if (IS_WINDOWS) {
			command.add("cmd.exe");
			command.add("/c");
			command.add("gradlew.bat");
		} else {
			command.add("./gradlew");
		}
		Collections.addAll(command, args);
		command.add("--no-watch-fs");

		Process process = new ProcessBuilder(command).directory(projectDir).redirectErrorStream(true).start();

		Thread thread = createOutputRedirectionThread(writer, process);
		thread.start();
		return process;
	}

	private static Thread createOutputRedirectionThread(Writer writer, Process process) {
		Thread thread = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					writer.write(line + System.lineSeparator());
					writer.flush();
				}
			} catch (IOException e) {
				// Ignore the exception, the writer might be closed
			}
		});
		thread.setDaemon(true);
		return thread;
	}
}