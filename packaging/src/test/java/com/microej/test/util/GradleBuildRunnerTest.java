/*
 * Java
 *
 * Copyright 2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

package com.microej.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests {@link GradleBuildRunner}, in particular the ownership of the writer passed to the builds.
 *
 * <p>
 * The builds run a stub {@code gradlew} script that prints one line and exits, so these tests need no real Gradle
 * project and run in the fast {@code unitTest} suite.
 */
public class GradleBuildRunnerTest {

	/** Timeout used when the expected text is supposed to be found. */
	private static final int FOUND_TIMEOUT_MILLIS = 5_000;

	/** Hard limit for a single test method, in seconds. */
	private static final int TEST_TIMEOUT_SECONDS = 30;

	/** Maximum time to wait for the process exit, in seconds. */
	private static final int PROCESS_EXIT_TIMEOUT_SECONDS = 10;

	/**
	 * Time left to a wrong close of the writer to happen after the process exit, in milliseconds.
	 *
	 * <p>
	 * A close triggered by the exit of the process runs asynchronously, so a test checking that the writer is left open
	 * must give such a close the time to happen.
	 */
	private static final long SETTLE_DELAY_MILLIS = 500;

	/** The line printed by the stub build. */
	private static final String STUB_BUILD_TRACE = "=== Stub build output ===";

	/** A line written by the test after the stub build exited. */
	private static final String AFTER_EXIT_TRACE = "Written after the process exit";

	private final GradleBuildRunner buildRunner = new GradleBuildRunner();

	private Path projectDir;

	/**
	 * Creates a temporary project directory containing the stub Gradle Wrapper.
	 *
	 * @throws IOException
	 *             if the stub Gradle Wrapper cannot be created
	 */
	@BeforeEach
	public void setUp() throws IOException {
		this.projectDir = Files.createTempDirectory("stub-gradle-project");
		Path gradlew = this.projectDir.resolve("gradlew");
		Files.writeString(gradlew, "#!/bin/sh\necho \"" + STUB_BUILD_TRACE + "\"\n");
		assertTrue(gradlew.toFile().setExecutable(true), "The stub gradlew must be executable.");
		Files.writeString(this.projectDir.resolve("gradlew.bat"), "@echo off\r\necho " + STUB_BUILD_TRACE + "\r\n");
	}

	/**
	 * Destroys the started processes and deletes the temporary project directory.
	 *
	 * @throws IOException
	 *             if the temporary project directory cannot be deleted
	 */
	@AfterEach
	public void tearDown() throws IOException {
		this.buildRunner.destroyAllProcesses();
		Files.deleteIfExists(this.projectDir.resolve("gradlew"));
		Files.deleteIfExists(this.projectDir.resolve("gradlew.bat"));
		Files.deleteIfExists(this.projectDir);
	}

	/**
	 * Checks that a synchronous build leaves the writer open: the caller keeps the ownership of the writer.
	 *
	 * @throws Exception
	 *             if the build cannot be started or the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testStartBuildLeavesWriterOpen() throws Exception {
		try (StringOutputBuffer output = new StringOutputBuffer()) {
			int exitCode = this.buildRunner.startBuild(this.projectDir.toFile(), output.getWriter());

			assertEquals(GradleBuildRunner.BUILD_SUCCESS, exitCode);
			assertTrue(output.waitUntilContains(STUB_BUILD_TRACE, FOUND_TIMEOUT_MILLIS),
					"The stub build output must be recorded.");

			writeLine(output, AFTER_EXIT_TRACE);
			assertTrue(output.waitUntilContains(AFTER_EXIT_TRACE, FOUND_TIMEOUT_MILLIS),
					"The writer must stay open after the build.");
		}
	}

	/**
	 * Checks that an asynchronous build leaves the writer open when the process exits, so a writer shared with another
	 * producer keeps recording.
	 *
	 * <p>
	 * The build is given a {@link CloseRecordingWriter}, so the check does not only rely on a later write being
	 * recorded: a close is detected even when it happens long after the exit of the process.
	 *
	 * @throws Exception
	 *             if the build cannot be started or the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testAsynchronousBuildLeavesWriterOpenOnExit() throws Exception {
		try (StringOutputBuffer output = new StringOutputBuffer()) {
			CloseRecordingWriter writer = new CloseRecordingWriter(output.getWriter());
			Process process = this.buildRunner.startAsynchronousBuild(this.projectDir.toFile(), writer);

			assertTrue(output.waitUntilContains(STUB_BUILD_TRACE, FOUND_TIMEOUT_MILLIS),
					"The stub build output must be recorded.");
			assertTrue(process.waitFor(PROCESS_EXIT_TIMEOUT_SECONDS, TimeUnit.SECONDS), "The stub build must exit.");
			Thread.sleep(SETTLE_DELAY_MILLIS);

			writeLine(output, AFTER_EXIT_TRACE);
			assertTrue(output.waitUntilContains(AFTER_EXIT_TRACE, FOUND_TIMEOUT_MILLIS),
					"The writer must stay open after the process exit.");
			assertFalse(writer.isCloseCalled(), "The build must never close the writer it was given.");
		}
	}

	/**
	 * Checks that starting a build in a directory without a Gradle Wrapper is rejected.
	 *
	 * @throws IOException
	 *             if the temporary directory cannot be created or deleted
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testMissingGradleWrapperFailsTheBuild() throws IOException {
		File emptyDir = Files.createTempDirectory("no-gradle-wrapper").toFile();
		try (StringOutputBuffer output = new StringOutputBuffer()) {
			int exitCode = this.buildRunner.startBuild(emptyDir, output.getWriter());

			assertEquals(GradleBuildRunner.BUILD_FAILURE, exitCode);
		} finally {
			Files.deleteIfExists(emptyDir.toPath());
		}
	}

	/**
	 * Writes the given text followed by a line separator to the given buffer, and flushes it.
	 *
	 * @param buffer
	 *            the buffer to write to
	 * @param text
	 *            the text to write
	 * @throws IOException
	 *             if the text cannot be written
	 */
	private static void writeLine(StringOutputBuffer buffer, String text) throws IOException {
		Writer writer = buffer.getWriter();
		writer.write(text + System.lineSeparator());
		writer.flush();
	}

	/**
	 * A writer that delegates every write to another writer, and records whether it has been closed.
	 *
	 * <p>
	 * The close is recorded and not delegated, so the writer given to the constructor stays open and the test can still
	 * read what has been written to it.
	 */
	private static class CloseRecordingWriter extends Writer {

		private final Writer delegate;

		/** Set by the thread that closes this writer, read by the test thread. */
		private volatile boolean closeCalled;

		CloseRecordingWriter(Writer delegate) {
			this.delegate = delegate;
		}

		@Override
		public void write(char[] buffer, int offset, int length) throws IOException {
			this.delegate.write(buffer, offset, length);
		}

		@Override
		public void flush() throws IOException {
			this.delegate.flush();
		}

		@Override
		public void close() {
			this.closeCalled = true;
		}

		/**
		 * Returns whether this writer has been closed.
		 *
		 * @return {@code true} if {@link #close()} has been called
		 */
		private boolean isCloseCalled() {
			return this.closeCalled;
		}
	}

}
