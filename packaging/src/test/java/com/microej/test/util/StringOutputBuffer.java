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
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides access to a {@link java.io.Writer} that can be used to record a text output, and convenience methods to
 * inspect the written content.
 *
 * <p>
 * The waits consume the written content: {@link #waitUntilContains(String, int)} and
 * {@link #waitUntilMatches(Pattern, int)} share one search index, and each call searches the text written after the
 * text matched by the previous call. Successive calls therefore wait for successive occurrences, and they must follow
 * the order in which the texts are written. A wait can also search the whole written content, for texts whose order is
 * not guaranteed, see {@link #waitUntilContains(String, int, boolean)} and
 * {@link #waitUntilMatches(Pattern, int, boolean)}. {@link #getContent()} always returns the whole written content.
 */
public class StringOutputBuffer implements AutoCloseable {

	static final String PROPERTY_CHECK_TIMEOUT = "string.output.wait.timeout";
	private static final int DEFAULT_CHECK_TIMEOUT_MILLIS = 60_000;

	/**
	 * Number of lines of the unmatched output logged when a wait is unsuccessful.
	 */
	private static final int TAIL_LINE_COUNT = 40;

	/**
	 * Maximum length, in characters, of a text matched by {@link #waitUntilMatches(Pattern, int, boolean)}.
	 *
	 * <p>
	 * {@link #waitUntilContains(String, int, boolean)} knows the exact length of the text it searches, so after each
	 * write it only searches again the end of the content, where a match can have started. A pattern has no maximum
	 * match length, so this value bounds it instead. Without a bound, the whole content has to be searched again after
	 * each write, which costs minutes of processing time on the output of a long build.
	 *
	 * <p>
	 * The first search of a wait is never bounded, so a text written before the wait started is always found. Only a
	 * match longer than this value can be missed, which then makes the wait fail on its timeout.
	 */
	private static final int MAX_MATCH_LENGTH = 8192;

	private static final Logger LOGGER = Logger.getLogger(StringOutputBuffer.class.getName());

	private final StringOutputWriter writer;
	private final Lock bufferLock = new ReentrantLock();
	private final Condition newData = bufferLock.newCondition();

	/**
	 * Index in the written content from which the next ordered search performed by
	 * {@link #waitUntilContains(String, int, boolean)} or {@link #waitUntilMatches(Pattern, int, boolean)} starts.
	 *
	 * <p>
	 * Guarded by {@link #bufferLock}.
	 */
	private int searchIndex;

	/**
	 * Creates a buffer that records the text written to its {@link java.io.Writer}.
	 */
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
	 * <p>
	 * By default the search starts at the current search index, which is the position right after the text matched by
	 * the previous successful call. Such an ordered wait never matches a text already matched by a previous call: two
	 * successive calls with the same expected text wait for two distinct occurrences of this text. When the expected
	 * text is found, the search index moves to the end of the matched text. When it is not found, the search index is
	 * left unchanged. Since only the text written after the search index is searched, the ordered calls must follow the
	 * order in which the expected texts are written.
	 *
	 * <p>
	 * Set the {@code fromStart} parameter to {@code true} to search the whole written content instead, from its
	 * beginning. Such a wait ignores the search index and leaves it unchanged, so it finds a text written before the
	 * previous match as well as a text already matched by a previous call, and it does not disturb the ordered waits
	 * that follow it. Use it for texts whose order is not guaranteed, typically traces written by two different threads.
	 *
	 * @param expected
	 *            the text to check in the written content
	 * @param timeoutMillis
	 *            the timeout period to wait before the check is considered unsuccessful
	 * @param fromStart
	 *            {@code true} to search the whole written content from its beginning, {@code false} to search only the
	 *            text written after the previously matched text
	 * @return {@code true} if the specified text is detected in the written content before the timeout elapses,
	 *         {@code false} otherwise
	 * @throws IllegalArgumentException
	 *             if the given timeout is negative
	 * @see #waitUntilContains(String, boolean)
	 */
	public boolean waitUntilContains(String expected, int timeoutMillis, boolean fromStart) {
		long timeoutTime = deadline(timeoutMillis);
		String awaited = "the expected text: " + expected;

		this.bufferLock.lock();
		try {
			// An ordered wait skips the text already matched, a wait from the start searches the whole content.
			int startIndex = fromStart ? 0 : this.searchIndex;
			// The text before this index cannot contain a new match: it has been either skipped or already searched by
			// this call.
			int searchFrom = startIndex;
			while (true) {
				int matchIndex = this.writer.indexOf(expected, searchFrom);
				if (matchIndex >= 0) {
					if (!fromStart) {
						this.searchIndex = matchIndex + expected.length();
					}
					return true;
				}

				// The next search only needs the text written from now on, plus the end of the current text, where a
				// match can start before the next write completes it.
				searchFrom = Math.max(searchFrom, this.writer.length() - expected.length() + 1);

				if (!awaitNewContent(timeoutTime, timeoutMillis, awaited, startIndex)) {
					return false;
				}
			}
		} finally {
			this.bufferLock.unlock();
		}
	}

	/**
	 * Waits until the written content contains the specified text, searching only the text written after the previously
	 * matched text.
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
	 * @throws IllegalArgumentException
	 *             if the given timeout is negative
	 * @see #waitUntilContains(String, int, boolean)
	 */
	public boolean waitUntilContains(String expected, int timeoutMillis) {
		return waitUntilContains(expected, timeoutMillis, false);
	}

	/**
	 * Waits until the written content contains the specified text.
	 *
	 * <p>
	 * This method blocks the calling thread until either the specified text is detected in the output or the timeout
	 * period elapses. This method uses the timeout delay set with the system property {@link #PROPERTY_CHECK_TIMEOUT}.
	 *
	 * @param expected
	 *            the text to check in the written content
	 * @param fromStart
	 *            {@code true} to search the whole written content from its beginning, {@code false} to search only the
	 *            text written after the previously matched text
	 * @return {@code true} if the specified text is detected in the written content before the timeout elapses,
	 *         {@code false} otherwise
	 * @see #waitUntilContains(String, int, boolean)
	 */
	public boolean waitUntilContains(String expected, boolean fromStart) {
		return waitUntilContains(expected, defaultTimeout(), fromStart);
	}

	/**
	 * Waits until the written content contains the specified text, searching only the text written after the previously
	 * matched text.
	 *
	 * <p>
	 * This method blocks the calling thread until either the specified text is detected in the output or the timeout
	 * period elapses. This method uses the timeout delay set with the system property {@link #PROPERTY_CHECK_TIMEOUT}.
	 *
	 * @param expected
	 *            the text to check in the written content
	 * @return {@code true} if the specified text is detected in the written content before the timeout elapses,
	 *         {@code false} otherwise
	 * @see #waitUntilContains(String, int, boolean)
	 */
	public boolean waitUntilContains(String expected) {
		return waitUntilContains(expected, defaultTimeout(), false);
	}

	/**
	 * Waits until the written content matches the specified pattern, and returns the match.
	 *
	 * <p>
	 * This method blocks the calling thread until either a match is detected in the output or the timeout period
	 * elapses.
	 *
	 * <p>
	 * The search starts at the current search index and moves it to the end of the match, and the {@code fromStart}
	 * parameter searches the whole written content from its beginning instead and leaves the search index unchanged,
	 * exactly as {@link #waitUntilContains(String, int, boolean)} does. When no match is found, the search index is
	 * left unchanged.
	 *
	 * <p>
	 * Use this method to read a value out of the text being awaited: the returned match gives access to the capture
	 * groups of the occurrence that has just been matched. There is no default value, so a text that is never written
	 * fails the check instead of being silently replaced.
	 *
	 * <p>
	 * A pattern that ends with a quantifier can match a partially written text, because the content is searched again
	 * after each write. For example, {@code port:\s*(\d+)} matches {@code 40} when the output is flushed in the middle
	 * of {@code port: 4001}. Anchor the pattern on the text that follows the value, normally the line break, so only a
	 * complete text can match:
	 *
	 * <pre>
	 * Pattern.compile("port:\\s*(\\d+)\\R"); // matches 4001
	 * Pattern.compile("port:\\s*(\\d+)"); // may match 40
	 * </pre>
	 *
	 * <p>
	 * A match must be shorter than {@value #MAX_MATCH_LENGTH} characters. This limit keeps the search efficient on the
	 * output of a long build. It is far above the length of a trace, so only a pattern written to span a large part of
	 * the output can reach it.
	 *
	 * @param pattern
	 *            the pattern to match in the written content
	 * @param timeoutMillis
	 *            the timeout period to wait before the check is considered unsuccessful
	 * @param fromStart
	 *            {@code true} to search the whole written content from its beginning, {@code false} to search only the
	 *            text written after the previously matched text
	 * @return the match, or {@code null} if the pattern does not match the written content before the timeout elapses
	 * @throws IllegalArgumentException
	 *             if the given timeout is negative
	 * @see #waitUntilMatches(Pattern, boolean)
	 */
	public MatchResult waitUntilMatches(Pattern pattern, int timeoutMillis, boolean fromStart) {
		long timeoutTime = deadline(timeoutMillis);
		String awaited = "the expected pattern: " + pattern + " (a match longer than " + MAX_MATCH_LENGTH
				+ " characters may have been missed, see MAX_MATCH_LENGTH)";

		this.bufferLock.lock();
		try {
			// An ordered wait skips the text already matched, a wait from the start searches the whole content. The
			// first search covers the whole content after this index, so a text written before this call is never
			// missed.
			int startIndex = fromStart ? 0 : this.searchIndex;
			int searchFrom = startIndex;
			while (true) {
				Matcher matcher = this.writer.matcher(pattern);
				if (matcher.find(searchFrom)) {
					if (!fromStart) {
						this.searchIndex = matcher.end();
					}
					return matcher.toMatchResult();
				}

				// A literal text has a known length, so waitUntilContains can rewind exactly and never miss a match.
				// A pattern has no such length: the exact index to search from is the index this wait started at,
				// which means searching a big part of the content again after each write. MAX_MATCH_LENGTH bounds the
				// rewind to the tail of the content instead.
				searchFrom = Math.max(searchFrom, this.writer.length() - MAX_MATCH_LENGTH + 1);

				if (!awaitNewContent(timeoutTime, timeoutMillis, awaited, startIndex)) {
					return null;
				}
			}
		} finally {
			this.bufferLock.unlock();
		}
	}

	/**
	 * Waits until the written content matches the specified pattern, and returns the match, searching only the text
	 * written after the previously matched text.
	 *
	 * <p>
	 * This method blocks the calling thread until either a match is detected in the output or the timeout period
	 * elapses.
	 *
	 * @param pattern
	 *            the pattern to match in the written content
	 * @param timeoutMillis
	 *            the timeout period to wait before the check is considered unsuccessful
	 * @return the match, or {@code null} if the pattern does not match the written content before the timeout elapses
	 * @throws IllegalArgumentException
	 *             if the given timeout is negative
	 * @see #waitUntilMatches(Pattern, int, boolean)
	 */
	public MatchResult waitUntilMatches(Pattern pattern, int timeoutMillis) {
		return waitUntilMatches(pattern, timeoutMillis, false);
	}

	/**
	 * Waits until the written content matches the specified pattern, and returns the match.
	 *
	 * <p>
	 * This method blocks the calling thread until either a match is detected in the output or the timeout period
	 * elapses. This method uses the timeout delay set with the system property {@link #PROPERTY_CHECK_TIMEOUT}.
	 *
	 * @param pattern
	 *            the pattern to match in the written content
	 * @param fromStart
	 *            {@code true} to search the whole written content from its beginning, {@code false} to search only the
	 *            text written after the previously matched text
	 * @return the match, or {@code null} if the pattern does not match the written content before the timeout elapses
	 * @see #waitUntilMatches(Pattern, int, boolean)
	 */
	public MatchResult waitUntilMatches(Pattern pattern, boolean fromStart) {
		return waitUntilMatches(pattern, defaultTimeout(), fromStart);
	}

	/**
	 * Waits until the written content matches the specified pattern, and returns the match, searching only the text
	 * written after the previously matched text.
	 *
	 * <p>
	 * This method blocks the calling thread until either a match is detected in the output or the timeout period
	 * elapses. This method uses the timeout delay set with the system property {@link #PROPERTY_CHECK_TIMEOUT}.
	 *
	 * @param pattern
	 *            the pattern to match in the written content
	 * @return the match, or {@code null} if the pattern does not match the written content before the timeout elapses
	 * @see #waitUntilMatches(Pattern, int, boolean)
	 */
	public MatchResult waitUntilMatches(Pattern pattern) {
		return waitUntilMatches(pattern, defaultTimeout(), false);
	}

	@Override
	public void close() throws IOException {
		this.writer.close();
	}

	/**
	 * Returns the timeout period used by the waits that do not take one, set with the system property
	 * {@link #PROPERTY_CHECK_TIMEOUT}.
	 *
	 * @return the default timeout period, in milliseconds
	 */
	private static int defaultTimeout() {
		return Integer.getInteger(PROPERTY_CHECK_TIMEOUT, DEFAULT_CHECK_TIMEOUT_MILLIS);
	}

	/**
	 * Validates the specified timeout and returns the time at which a wait starting now must give up.
	 *
	 * @param timeoutMillis
	 *            the timeout period of the wait
	 * @return the time, in milliseconds, at which the wait must give up
	 * @throws IllegalArgumentException
	 *             if the specified timeout is negative
	 */
	private static long deadline(int timeoutMillis) {
		if (timeoutMillis < 0) {
			throw new IllegalArgumentException(
					"Invalid value for timeout: " + timeoutMillis + ". Expected a value greater or equal to 0.");
		}
		return System.currentTimeMillis() + timeoutMillis;
	}

	/**
	 * Waits until new content is written, and returns whether the awaited content must be searched again.
	 *
	 * <p>
	 * This method must be called with {@link #bufferLock} held. It logs a warning with the unmatched output when the
	 * wait cannot succeed anymore, either because the output is closed, because the timeout has elapsed, or because the
	 * calling thread has been interrupted.
	 *
	 * @param timeoutTime
	 *            the time, in milliseconds, at which the wait must give up
	 * @param timeoutMillis
	 *            the timeout period of the wait, only used in the warning reporting the unsuccessful wait
	 * @param awaited
	 *            the description of the awaited content, only used in the warning reporting the unsuccessful wait
	 * @param searchStart
	 *            the index at which the wait started its search, only used in the warning reporting the unsuccessful
	 *            wait
	 * @return {@code true} if the awaited content must be searched again, {@code false} if the wait is unsuccessful
	 */
	private boolean awaitNewContent(long timeoutTime, int timeoutMillis, String awaited, int searchStart) {
		if (!this.writer.isOpen()) {
			LOGGER.warning("The output is closed and does not contain " + awaited + "." + unmatchedTail(searchStart));
			return false;
		}

		long remainingTime = timeoutTime - System.currentTimeMillis();
		if (remainingTime <= 0) {
			LOGGER.warning("Waiting time exceeded timeout limit (" + timeoutMillis + " ms) for " + awaited + "."
					+ unmatchedTail(searchStart));
			return false;
		}

		try {
			this.newData.await(remainingTime, TimeUnit.MILLISECONDS);
			return true;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.log(Level.WARNING, "Interrupted while waiting for new text input.", e);
			return false;
		}
	}

	/**
	 * Returns the end of the content searched by an unsuccessful wait, to be appended to the warning reporting it.
	 *
	 * <p>
	 * The result is limited to the last {@link #TAIL_LINE_COUNT} lines, so a long output does not fill the logs. This
	 * method is only called when a wait fails.
	 *
	 * @param searchStart
	 *            the index at which the unsuccessful wait started its search
	 * @return the end of the unmatched content, prefixed by a line separator
	 */
	private String unmatchedTail(int searchStart) {
		String content = getContent();
		String unmatched = content.substring(Math.min(searchStart, content.length()));

		int tailStart = unmatched.length();
		for (int lineCount = 0; lineCount < TAIL_LINE_COUNT; lineCount++) {
			int previousLineEnd = unmatched.lastIndexOf('\n', tailStart - 1);
			if (previousLineEnd < 0) {
				tailStart = 0;
				break;
			}
			tailStart = previousLineEnd;
		}
		String tail = (tailStart == 0) ? unmatched : unmatched.substring(tailStart + 1);

		String lineSeparator = System.lineSeparator();
		return lineSeparator + "Unmatched output (last " + tail.length() + " of " + unmatched.length()
				+ " characters, up to " + TAIL_LINE_COUNT + " lines):" + lineSeparator + tail;
	}

	private class StringOutputWriter extends Writer {

		private final StringWriter writer;
		private final PrintWriter out;
		private volatile boolean closed;

		/**
		 * Whether the warning reporting dropped writes has been logged. Guarded by {@link #bufferLock}.
		 */
		private boolean droppedWriteReported;

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
					reportDroppedWrite();
					return;
				}
				this.writer.write(cbuf, off, len);
				this.out.write(cbuf, off, len);
			} finally {
				bufferLock.unlock();
			}
		}

		/**
		 * Logs a warning the first time a write is dropped because the output is closed.
		 *
		 * <p>
		 * This method must be called with {@link #bufferLock} held. A single warning is logged per buffer, so a
		 * producer that keeps writing after the close does not fill the logs.
		 */
		private void reportDroppedWrite() {
			if (!this.droppedWriteReported) {
				this.droppedWriteReported = true;
				LOGGER.warning("The output is closed: the text written to it from now on is dropped.");
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
				// Flush the echo writer, never close it: closing it would close System.out itself.
				this.out.flush();
				newData.signalAll();
			} catch (IOException e) {
				// can't happen with StringWriter
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

		/**
		 * Returns the index of the first occurrence of the given text, starting the search at the given index.
		 *
		 * <p>
		 * The written text is searched in place, so no copy of the whole content is made.
		 *
		 * @param text
		 *            the text to search
		 * @param fromIndex
		 *            the index to start the search at
		 * @return the index of the first occurrence of the text, or {@code -1} if the text is not found
		 */
		private int indexOf(String text, int fromIndex) {
			return this.writer.getBuffer().indexOf(text, fromIndex);
		}

		/**
		 * Returns a matcher of the given pattern on the written text.
		 *
		 * <p>
		 * The written text is matched in place, so no copy of the whole content is made.
		 *
		 * @param pattern
		 *            the pattern to match
		 * @return a matcher of the given pattern on the written text
		 */
		private Matcher matcher(Pattern pattern) {
			return pattern.matcher(this.writer.getBuffer());
		}

		/**
		 * Returns the number of characters written.
		 *
		 * @return the number of characters written
		 */
		private int length() {
			return this.writer.getBuffer().length();
		}
	}
}
