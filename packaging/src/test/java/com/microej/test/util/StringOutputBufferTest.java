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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests {@link StringOutputBuffer}.
 *
 * <p>
 * Always pass an explicit timeout to the waits in these tests, so use only
 * {@link StringOutputBuffer#waitUntilContains(String, int)},
 * {@link StringOutputBuffer#waitUntilContains(String, int, boolean)},
 * {@link StringOutputBuffer#waitUntilMatches(Pattern, int)} and
 * {@link StringOutputBuffer#waitUntilMatches(Pattern, int, boolean)}. The default timeout is several minutes long, so a
 * regression would block the build instead of failing it.
 */
public class StringOutputBufferTest {

	/** Timeout used when the expected text is supposed to be found. */
	private static final int FOUND_TIMEOUT_MILLIS = 5_000;

	/** Timeout used when the expected text is supposed to be missing, to keep the test fast. */
	private static final int MISSING_TIMEOUT_MILLIS = 300;

	/** Hard limit for a single test method, in seconds. */
	private static final int TEST_TIMEOUT_SECONDS = 30;

	/** Maximum time a wait on a closed output may take, in milliseconds. */
	private static final int CLOSED_OUTPUT_MAX_WAIT_MILLIS = 1_000;

	private static final String DONE_TRACE = "=== Done ===";
	private static final String FIRST_TRACE = "First trace";
	private static final String SECOND_TRACE = "Second trace";
	private static final String MISSING_TRACE = "Never written trace";

	/**
	 * Matches a trace carrying a value, and captures the value.
	 *
	 * <p>
	 * The pattern is anchored on the line break, so it cannot match a value that is only partially written.
	 */
	private static final Pattern VALUE_PATTERN = Pattern.compile("value=(\\d+)\\R");

	/** Matches a trace that is never written. */
	private static final Pattern MISSING_PATTERN = Pattern.compile("never written value=(\\d+)\\R");

	private static final String FIRST_VALUE = "4001";
	private static final String SECOND_VALUE = "4002";

	/** A trace of no interest, written to fill the output. */
	private static final String FILLER_TRACE = "> Task :kernel-linux-x86:buildExecutable unrelated output";

	/**
	 * Number of characters of filler written to exceed the length of the content that the pattern wait searches again
	 * after each write. It must stay above the {@code MAX_MATCH_LENGTH} of {@link StringOutputBuffer}.
	 */
	private static final int FILLER_LENGTH = 16384;

	private static final int PRODUCED_LINE_COUNT = 10;
	private static final long PRODUCER_DELAY_MILLIS = 5;

	/**
	 * Checks that two successive waits for the same text wait for two distinct occurrences of this text.
	 *
	 * <p>
	 * This is the case of two applications printing the same trace: the second wait must not be satisfied by the trace
	 * of the first application.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testSameTextTwiceRequiresTwoOccurrences() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, DONE_TRACE);

			assertTrue(buffer.waitUntilContains(DONE_TRACE, FOUND_TIMEOUT_MILLIS),
					"The first occurrence must be found.");
			assertFalse(buffer.waitUntilContains(DONE_TRACE, MISSING_TIMEOUT_MILLIS),
					"The first occurrence must not be matched twice.");

			writeLine(buffer, DONE_TRACE);

			assertTrue(buffer.waitUntilContains(DONE_TRACE, FOUND_TIMEOUT_MILLIS),
					"The second occurrence must be found.");
		}
	}

	/**
	 * Checks that successive waits for different texts are all satisfied.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testSuccessiveDifferentTexts() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, FIRST_TRACE);
			writeLine(buffer, SECOND_TRACE);

			assertTrue(buffer.waitUntilContains(FIRST_TRACE, FOUND_TIMEOUT_MILLIS));
			assertTrue(buffer.waitUntilContains(SECOND_TRACE, FOUND_TIMEOUT_MILLIS));
		}
	}

	/**
	 * Checks that the waits must follow the order in which the texts are written.
	 *
	 * <p>
	 * This documents the expected side effect of the search index: a text written before the previously matched text is
	 * not found anymore.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testWaitsMustFollowWriteOrder() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, SECOND_TRACE);
			writeLine(buffer, FIRST_TRACE);

			assertTrue(buffer.waitUntilContains(FIRST_TRACE, FOUND_TIMEOUT_MILLIS));
			assertFalse(buffer.waitUntilContains(SECOND_TRACE, MISSING_TIMEOUT_MILLIS),
					"A text written before the previous match must not be found.");
		}
	}

	/**
	 * Checks that a text split over two writes is found.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testMatchAcrossWriteBoundary() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			write(buffer, "=== Do");

			assertFalse(buffer.waitUntilContains(DONE_TRACE, MISSING_TIMEOUT_MILLIS),
					"An incomplete text must not be found.");

			write(buffer, "ne ===");

			assertTrue(buffer.waitUntilContains(DONE_TRACE, FOUND_TIMEOUT_MILLIS));
		}
	}

	/**
	 * Checks that the search index does not truncate the content returned by {@link StringOutputBuffer#getContent()}.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testGetContentReturnsFullContent() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, FIRST_TRACE);
			writeLine(buffer, SECOND_TRACE);

			assertTrue(buffer.waitUntilContains(SECOND_TRACE, FOUND_TIMEOUT_MILLIS));

			String content = buffer.getContent();
			assertTrue(content.contains(FIRST_TRACE), "The whole content must be returned.");
			assertTrue(content.contains(SECOND_TRACE), "The whole content must be returned.");
		}
	}

	/**
	 * Checks that an unsuccessful wait leaves the search index unchanged, so a later wait still finds the text.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testSearchIndexUnchangedOnTimeout() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, FIRST_TRACE);

			assertFalse(buffer.waitUntilContains(MISSING_TRACE, MISSING_TIMEOUT_MILLIS));

			assertTrue(buffer.waitUntilContains(FIRST_TRACE, FOUND_TIMEOUT_MILLIS),
					"A failed wait must not consume the content.");
		}
	}

	/**
	 * Checks that a wait from the start of the content matches a text already matched by a previous wait.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testFromStartMatchesAlreadyMatchedText() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, FIRST_TRACE);

			assertTrue(buffer.waitUntilContains(FIRST_TRACE, FOUND_TIMEOUT_MILLIS));

			assertTrue(buffer.waitUntilContains(FIRST_TRACE, FOUND_TIMEOUT_MILLIS, true),
					"A wait from the start must match a text already matched.");
		}
	}

	/**
	 * Checks that a wait from the start of the content matches a text written before the previously matched text, and
	 * that it does not rewind the search index.
	 *
	 * <p>
	 * This is the case of two threads writing their traces independently: the trace awaited last can be written first.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testFromStartMatchesTextWrittenBeforePreviousMatch() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, SECOND_TRACE);
			writeLine(buffer, FIRST_TRACE);

			assertTrue(buffer.waitUntilContains(FIRST_TRACE, FOUND_TIMEOUT_MILLIS));

			assertTrue(buffer.waitUntilContains(SECOND_TRACE, FOUND_TIMEOUT_MILLIS, true),
					"A wait from the start must match a text written before the previous match.");

			assertFalse(buffer.waitUntilContains(SECOND_TRACE, MISSING_TIMEOUT_MILLIS),
					"A wait from the start must not rewind the search index.");
		}
	}

	/**
	 * Checks that a wait from the start of the content does not consume the text it matched, so the ordered waits that
	 * follow it are not disturbed.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testFromStartDoesNotConsumeMatchedText() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, FIRST_TRACE);
			writeLine(buffer, SECOND_TRACE);
			writeLine(buffer, DONE_TRACE);

			assertTrue(buffer.waitUntilContains(FIRST_TRACE, FOUND_TIMEOUT_MILLIS));
			assertTrue(buffer.waitUntilContains(DONE_TRACE, FOUND_TIMEOUT_MILLIS, true));

			assertTrue(buffer.waitUntilContains(SECOND_TRACE, FOUND_TIMEOUT_MILLIS),
					"A wait from the start must not consume the text written before the text it matched.");
		}
	}

	/**
	 * Checks that a wait from the start of the content waits for a text that is not written yet.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testFromStartWaitsForTextWrittenLater() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, FIRST_TRACE);

			assertTrue(buffer.waitUntilContains(FIRST_TRACE, FOUND_TIMEOUT_MILLIS));
			assertFalse(buffer.waitUntilContains(DONE_TRACE, MISSING_TIMEOUT_MILLIS, true),
					"A text that is not written yet must not be found.");

			writeLine(buffer, DONE_TRACE);

			assertTrue(buffer.waitUntilContains(DONE_TRACE, FOUND_TIMEOUT_MILLIS, true));
		}
	}

	/**
	 * Checks that the lines written by another thread are found one by one, in the order they are written.
	 *
	 * @throws Exception
	 *             if the text cannot be written or the producer thread cannot be joined
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testConcurrentProducer() throws Exception {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			Thread producer = new Thread(() -> {
				try {
					for (int i = 0; i < PRODUCED_LINE_COUNT; i++) {
						writeLine(buffer, producedLine(i));
						Thread.sleep(PRODUCER_DELAY_MILLIS);
					}
				} catch (IOException e) {
					throw new IllegalStateException("Could not write the produced lines.", e);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
			producer.setDaemon(true);
			producer.start();

			for (int i = 0; i < PRODUCED_LINE_COUNT; i++) {
				assertTrue(buffer.waitUntilContains(producedLine(i), FOUND_TIMEOUT_MILLIS),
						"The line " + i + " must be found.");
			}

			producer.join();
		}
	}

	/**
	 * Checks that a text written before the output is closed is still found after the close.
	 *
	 * <p>
	 * The output is closed as soon as the process writing to it exits, which can happen before the test thread checks
	 * the content.
	 *
	 * @throws IOException
	 *             if the text cannot be written or the buffer cannot be closed
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testWaitAfterCloseFindsUnconsumedText() throws IOException {
		StringOutputBuffer buffer = new StringOutputBuffer();
		writeLine(buffer, FIRST_TRACE);
		buffer.close();

		assertTrue(buffer.waitUntilContains(FIRST_TRACE, FOUND_TIMEOUT_MILLIS),
				"A text written before the close must still be found.");
	}

	/**
	 * Checks that a wait on a closed output returns without waiting for the timeout.
	 *
	 * @throws IOException
	 *             if the buffer cannot be closed
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testWaitAfterCloseWithoutTextReturnsImmediately() throws IOException {
		StringOutputBuffer buffer = new StringOutputBuffer();
		buffer.close();

		long startTime = System.currentTimeMillis();
		assertFalse(buffer.waitUntilContains(MISSING_TRACE, FOUND_TIMEOUT_MILLIS));
		long elapsedTime = System.currentTimeMillis() - startTime;

		assertTrue(elapsedTime < CLOSED_OUTPUT_MAX_WAIT_MILLIS,
				"The wait on a closed output took " + elapsedTime + " ms.");
	}

	/**
	 * Checks that a write to a closed output is dropped, and that a single warning reports the dropped writes.
	 *
	 * <p>
	 * This is the case of a producer that keeps writing after its buffer has been closed, typically a build whose
	 * writer has been closed on the exit of another build: the log must tell that text is being dropped, once, so the
	 * missing text can be diagnosed without filling the logs.
	 *
	 * @throws IOException
	 *             if the text cannot be written or the buffer cannot be closed
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testWriteAfterCloseIsDroppedAndLogsOneWarning() throws IOException {
		Logger logger = Logger.getLogger(StringOutputBuffer.class.getName());
		List<LogRecord> records = new ArrayList<>();
		Handler handler = newRecordingHandler(records);
		logger.addHandler(handler);
		try {
			StringOutputBuffer buffer = new StringOutputBuffer();
			writeLine(buffer, FIRST_TRACE);
			buffer.close();

			writeLine(buffer, SECOND_TRACE);
			writeLine(buffer, DONE_TRACE);

			String content = buffer.getContent();
			assertTrue(content.contains(FIRST_TRACE), "The text written before the close must be kept.");
			assertFalse(content.contains(SECOND_TRACE), "The text written after the close must be dropped.");

			List<String> warnings = records.stream()
					.filter(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
					.map(LogRecord::getMessage).filter(message -> message.contains("dropped"))
					.collect(Collectors.toList());
			assertEquals(1, warnings.size(),
					"Exactly one warning must report the dropped writes, but the log records were: "
							+ messages(records));
		} finally {
			logger.removeHandler(handler);
		}
	}

	/**
	 * Checks that closing the output does not close {@code System.out}, which the output echoes to.
	 *
	 * @throws IOException
	 *             if the text cannot be written or the buffer cannot be closed
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testCloseLeavesSystemOutOpen() throws IOException {
		PrintStream originalOut = System.out;
		AtomicBoolean systemOutClosed = new AtomicBoolean();
		System.setOut(new PrintStream(originalOut) {
			@Override
			public void close() {
				// Record the close instead of closing the real stream, so this test cannot break System.out.
				systemOutClosed.set(true);
			}
		});
		try {
			StringOutputBuffer buffer = new StringOutputBuffer();
			writeLine(buffer, FIRST_TRACE);
			buffer.close();

			assertFalse(systemOutClosed.get(), "Closing the output must not close System.out.");
		} finally {
			System.setOut(originalOut);
		}
	}

	/**
	 * Checks that a negative timeout is rejected.
	 *
	 * @throws IOException
	 *             if the buffer cannot be closed
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testNegativeTimeoutThrows() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			assertThrows(IllegalArgumentException.class, () -> buffer.waitUntilContains(FIRST_TRACE, -1));
		}
	}

	/**
	 * Checks that a zero timeout checks the already written content once.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testZeroTimeoutChecksWrittenContent() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, FIRST_TRACE);

			assertTrue(buffer.waitUntilContains(FIRST_TRACE, 0));
			assertFalse(buffer.waitUntilContains(MISSING_TRACE, 0));
		}
	}

	/**
	 * Checks that an unsuccessful wait logs the output that has not been matched, to tell what was written instead of
	 * the expected text.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testTimeoutLogsUnmatchedOutput() throws IOException {
		Logger logger = Logger.getLogger(StringOutputBuffer.class.getName());
		List<LogRecord> records = new ArrayList<>();
		Handler handler = newRecordingHandler(records);
		logger.addHandler(handler);
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, FIRST_TRACE);

			assertFalse(buffer.waitUntilContains(MISSING_TRACE, MISSING_TIMEOUT_MILLIS));

			// Keep the warnings reporting the wait that just failed, so an unrelated log record cannot satisfy the
			// check below.
			List<String> warnings = records.stream()
					.filter(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
					.map(LogRecord::getMessage).filter(message -> message.contains(MISSING_TRACE))
					.collect(Collectors.toList());

			assertEquals(1, warnings.size(),
					"Exactly one warning must report the failed wait, but the log records were: " + messages(records));
			assertTrue(warnings.get(0).contains(FIRST_TRACE),
					"The warning reporting the failed wait must contain the unmatched output, but it was: "
							+ warnings.get(0));
		} finally {
			logger.removeHandler(handler);
		}
	}

	/**
	 * Checks that two successive waits for the same pattern match two distinct occurrences.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testSamePatternTwiceRequiresTwoOccurrences() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, valueTrace(FIRST_VALUE));

			assertNotNull(buffer.waitUntilMatches(VALUE_PATTERN, FOUND_TIMEOUT_MILLIS),
					"The first occurrence must be found.");
			assertNull(buffer.waitUntilMatches(VALUE_PATTERN, MISSING_TIMEOUT_MILLIS),
					"The first occurrence must not be matched twice.");

			writeLine(buffer, valueTrace(SECOND_VALUE));

			assertNotNull(buffer.waitUntilMatches(VALUE_PATTERN, FOUND_TIMEOUT_MILLIS),
					"The second occurrence must be found.");
		}
	}

	/**
	 * Checks that the capture groups come from the occurrence that has just been awaited.
	 *
	 * <p>
	 * This is the reason to use a pattern instead of searching the whole content: the value read is the one of the
	 * occurrence awaited, not the one of the first occurrence.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testCaptureGroupsComeFromMatchedOccurrence() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, valueTrace(FIRST_VALUE));
			writeLine(buffer, valueTrace(SECOND_VALUE));

			MatchResult firstMatch = buffer.waitUntilMatches(VALUE_PATTERN, FOUND_TIMEOUT_MILLIS);
			assertNotNull(firstMatch);
			assertEquals(FIRST_VALUE, firstMatch.group(1), "The first wait must return the first value.");

			MatchResult secondMatch = buffer.waitUntilMatches(VALUE_PATTERN, FOUND_TIMEOUT_MILLIS);
			assertNotNull(secondMatch);
			assertEquals(SECOND_VALUE, secondMatch.group(1), "The second wait must return the second value.");
		}
	}

	/**
	 * Checks that a pattern that does not match returns no match instead of a default value.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testUnmatchedPatternReturnsNoMatch() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, valueTrace(FIRST_VALUE));

			assertNull(buffer.waitUntilMatches(MISSING_PATTERN, MISSING_TIMEOUT_MILLIS),
					"A pattern that does not match must not return a value.");
		}
	}

	/**
	 * Checks that an unsuccessful pattern wait leaves the search index unchanged, so a later wait still matches.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testSearchIndexUnchangedOnPatternTimeout() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, valueTrace(FIRST_VALUE));

			assertNull(buffer.waitUntilMatches(MISSING_PATTERN, MISSING_TIMEOUT_MILLIS));

			assertNotNull(buffer.waitUntilMatches(VALUE_PATTERN, FOUND_TIMEOUT_MILLIS),
					"A failed wait must not consume the content.");
		}
	}

	/**
	 * Checks that a pattern wait from the start of the content matches a text already matched by a previous wait, and
	 * that it does not move the search index.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testFromStartMatchesAlreadyMatchedPattern() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, valueTrace(FIRST_VALUE));
			writeLine(buffer, valueTrace(SECOND_VALUE));

			assertNotNull(buffer.waitUntilMatches(VALUE_PATTERN, FOUND_TIMEOUT_MILLIS));

			MatchResult match = buffer.waitUntilMatches(VALUE_PATTERN, FOUND_TIMEOUT_MILLIS, true);
			assertNotNull(match, "A pattern wait from the start must match a text already matched.");
			assertEquals(FIRST_VALUE, match.group(1), "The first occurrence must be matched again.");

			MatchResult nextMatch = buffer.waitUntilMatches(VALUE_PATTERN, FOUND_TIMEOUT_MILLIS);
			assertNotNull(nextMatch, "A pattern wait from the start must not consume the content.");
			assertEquals(SECOND_VALUE, nextMatch.group(1),
					"The ordered wait that follows must match the next occurrence.");
		}
	}

	/**
	 * Checks that the text waits and the pattern waits share one search index, so they can be mixed to follow the order
	 * in which the traces are written.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testSearchIndexSharedByTextAndPatternWaits() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, FIRST_TRACE);
			writeLine(buffer, valueTrace(FIRST_VALUE));
			writeLine(buffer, SECOND_TRACE);
			writeLine(buffer, valueTrace(SECOND_VALUE));

			assertTrue(buffer.waitUntilContains(FIRST_TRACE, FOUND_TIMEOUT_MILLIS));

			MatchResult firstMatch = buffer.waitUntilMatches(VALUE_PATTERN, FOUND_TIMEOUT_MILLIS);
			assertNotNull(firstMatch);
			assertEquals(FIRST_VALUE, firstMatch.group(1), "The pattern wait must start where the text wait stopped.");

			assertTrue(buffer.waitUntilContains(SECOND_TRACE, FOUND_TIMEOUT_MILLIS),
					"The text wait must start where the pattern wait stopped.");

			MatchResult secondMatch = buffer.waitUntilMatches(VALUE_PATTERN, FOUND_TIMEOUT_MILLIS);
			assertNotNull(secondMatch);
			assertEquals(SECOND_VALUE, secondMatch.group(1));
		}
	}

	/**
	 * Checks that a pattern anchored on the line break only matches once the whole value is written.
	 *
	 * <p>
	 * This is the documented way to avoid matching a partially written value, which happens when the output is flushed
	 * in the middle of a trace.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testAnchoredPatternMatchesAcrossWriteBoundary() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			// Flush the trace in the middle of the value, as a process writing to the output can do.
			write(buffer, "value=400");

			assertNull(buffer.waitUntilMatches(VALUE_PATTERN, MISSING_TIMEOUT_MILLIS),
					"A partially written value must not be matched.");

			write(buffer, "1" + System.lineSeparator());

			MatchResult match = buffer.waitUntilMatches(VALUE_PATTERN, FOUND_TIMEOUT_MILLIS);
			assertNotNull(match);
			assertEquals(FIRST_VALUE, match.group(1), "The whole value must be matched.");
		}
	}

	/**
	 * Checks that the first search of a pattern wait covers the whole content written before the call.
	 *
	 * <p>
	 * The searches that follow a write are bounded to the end of the content, to keep the search efficient on the output
	 * of a long build. This checks that the bound does not apply to the first search: a value written long before the
	 * wait, followed by more output than the bound, must still be matched.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testFirstSearchCoversContentWrittenBeforeTheWait() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, valueTrace(FIRST_VALUE));

			int fillerLineCount = FILLER_LENGTH / FILLER_TRACE.length() + 1;
			for (int i = 0; i < fillerLineCount; i++) {
				writeLine(buffer, FILLER_TRACE);
			}
			assertTrue(buffer.getContent().length() > FILLER_LENGTH, "The filler must exceed the bounded search.");

			MatchResult match = buffer.waitUntilMatches(VALUE_PATTERN, FOUND_TIMEOUT_MILLIS);
			assertNotNull(match, "A value written before the wait must be matched whatever follows it.");
			assertEquals(FIRST_VALUE, match.group(1));
		}
	}

	/**
	 * Checks that a pattern matching text written before the output is closed still matches after the close.
	 *
	 * @throws IOException
	 *             if the text cannot be written or the buffer cannot be closed
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testWaitUntilMatchesAfterCloseFindsUnconsumedMatch() throws IOException {
		StringOutputBuffer buffer = new StringOutputBuffer();
		writeLine(buffer, valueTrace(FIRST_VALUE));
		buffer.close();

		MatchResult match = buffer.waitUntilMatches(VALUE_PATTERN, FOUND_TIMEOUT_MILLIS);
		assertNotNull(match, "A value written before the close must still be matched.");
		assertEquals(FIRST_VALUE, match.group(1));
	}

	/**
	 * Checks that a pattern wait on a closed output returns without waiting for the timeout.
	 *
	 * @throws IOException
	 *             if the buffer cannot be closed
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testWaitUntilMatchesAfterCloseReturnsImmediately() throws IOException {
		StringOutputBuffer buffer = new StringOutputBuffer();
		buffer.close();

		long startTime = System.currentTimeMillis();
		assertNull(buffer.waitUntilMatches(VALUE_PATTERN, FOUND_TIMEOUT_MILLIS));
		long elapsedTime = System.currentTimeMillis() - startTime;

		assertTrue(elapsedTime < CLOSED_OUTPUT_MAX_WAIT_MILLIS,
				"The wait on a closed output took " + elapsedTime + " ms.");
	}

	/**
	 * Checks that a negative timeout is rejected by the pattern wait.
	 *
	 * @throws IOException
	 *             if the buffer cannot be closed
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testNegativeTimeoutThrowsForPattern() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			assertThrows(IllegalArgumentException.class, () -> buffer.waitUntilMatches(VALUE_PATTERN, -1));
		}
	}

	/**
	 * Checks that a zero timeout checks the already written content once.
	 *
	 * @throws IOException
	 *             if the text cannot be written
	 */
	@Test
	@Timeout(TEST_TIMEOUT_SECONDS)
	public void testZeroTimeoutChecksWrittenContentForPattern() throws IOException {
		try (StringOutputBuffer buffer = new StringOutputBuffer()) {
			writeLine(buffer, valueTrace(FIRST_VALUE));

			assertNotNull(buffer.waitUntilMatches(VALUE_PATTERN, 0));
			assertNull(buffer.waitUntilMatches(MISSING_PATTERN, 0));
		}
	}

	/**
	 * Returns a handler that records the published log records into the given list.
	 *
	 * @param records
	 *            the list to record the published log records into
	 * @return the recording handler
	 */
	private static Handler newRecordingHandler(List<LogRecord> records) {
		return new Handler() {
			@Override
			public void publish(LogRecord record) {
				records.add(record);
			}

			@Override
			public void flush() {
				// Nothing to flush: the records are kept in memory.
			}

			@Override
			public void close() {
				// Nothing to close: the records are kept in memory.
			}
		};
	}

	/**
	 * Returns the level and the message of the given log records, to be used in an assertion message.
	 *
	 * @param records
	 *            the log records to describe
	 * @return the description of the given log records
	 */
	private static List<String> messages(List<LogRecord> records) {
		return records.stream().map(record -> record.getLevel() + ": " + record.getMessage())
				.collect(Collectors.toList());
	}

	/**
	 * Returns the text of the line produced at the given index.
	 *
	 * @param index
	 *            the index of the produced line
	 * @return the text of the produced line
	 */
	private static String producedLine(int index) {
		return "line-" + index;
	}

	/**
	 * Returns a trace carrying the given value, matched by {@link #VALUE_PATTERN}.
	 *
	 * @param value
	 *            the value carried by the trace
	 * @return the text of the trace
	 */
	private static String valueTrace(String value) {
		return "value=" + value;
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
		write(buffer, text + System.lineSeparator());
	}

	/**
	 * Writes the given text to the given buffer as it is, and flushes it so the waiting threads are notified.
	 *
	 * @param buffer
	 *            the buffer to write to
	 * @param text
	 *            the text to write
	 * @throws IOException
	 *             if the text cannot be written
	 */
	private static void write(StringOutputBuffer buffer, String text) throws IOException {
		Writer writer = buffer.getWriter();
		writer.write(text);
		writer.flush();
	}

}
