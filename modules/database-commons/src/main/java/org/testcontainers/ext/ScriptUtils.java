/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testcontainers.ext;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.delegate.DatabaseDelegate;

import javax.script.ScriptException;
import java.util.LinkedList;
import java.util.List;

/**
 * This is a modified version of the Spring-JDBC ScriptUtils class, adapted to reduce
 * dependencies and slightly alter the API.
 *
 * Generic utility methods for working with SQL scripts. Mainly for internal use
 * within the framework.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Dave Syer
 * @author Chris Beams
 * @author Oliver Gierke
 * @author Chris Baldwin
 * @since 4.0.3
 */
public abstract class ScriptUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptUtils.class);

	/**
	 * Default statement separator within SQL scripts.
	 */
	public static final String DEFAULT_STATEMENT_SEPARATOR = ";";

	/**
	 * Fallback statement separator within SQL scripts.
	 * <p>Used if neither a custom defined separator nor the
	 * {@link #DEFAULT_STATEMENT_SEPARATOR} is present in a given script.
	 */
	public static final String FALLBACK_STATEMENT_SEPARATOR = "\n";

	/**
	 * Default prefix for line comments within SQL scripts.
	 */
	public static final String DEFAULT_COMMENT_PREFIX = "--";

	/**
	 * Default start delimiter for block comments within SQL scripts.
	 */
	public static final String DEFAULT_BLOCK_COMMENT_START_DELIMITER = "/*";

	/**
	 * Default end delimiter for block comments within SQL scripts.
	 */
	public static final String DEFAULT_BLOCK_COMMENT_END_DELIMITER = "*/";


	/**
	 * Prevent instantiation of this utility class.
	 */
	private ScriptUtils() {
		/* no-op */
	}

    /**
	 * Split an SQL script into separate statements delimited by the provided
	 * separator string. Each individual statement will be added to the provided
	 * {@code List}.
	 * <p>Within the script, the provided {@code commentPrefix} will be honored:
	 * any text beginning with the comment prefix and extending to the end of the
	 * line will be omitted from the output. Similarly, the provided
	 * {@code blockCommentStartDelimiter} and {@code blockCommentEndDelimiter}
	 * delimiters will be honored: any text enclosed in a block comment will be
	 * omitted from the output. In addition, multiple adjacent whitespace characters
	 * will be collapsed into a single space.
	 * @param resource the resource from which the script was read
	 * @param script the SQL script; never {@code null} or empty
	 * @param separator text separating each statement &mdash; typically a ';' or
	 * newline character; never {@code null}
	 * @param commentPrefix the prefix that identifies SQL line comments &mdash;
	 * typically "--"; never {@code null} or empty
	 * @param blockCommentStartDelimiter the <em>start</em> block comment delimiter;
	 * never {@code null} or empty
	 * @param blockCommentEndDelimiter the <em>end</em> block comment delimiter;
	 * never {@code null} or empty
	 * @param statements the list that will contain the individual statements
	 */
	public static void splitSqlScript(String resource, String script, String separator, String commentPrefix,
                                      String blockCommentStartDelimiter, String blockCommentEndDelimiter, List<String> statements) {

		checkArgument(StringUtils.isNotEmpty(script), "script must not be null or empty");
		checkArgument(separator != null, "separator must not be null");
		checkArgument(StringUtils.isNotEmpty(commentPrefix), "commentPrefix must not be null or empty");
		checkArgument(StringUtils.isNotEmpty(blockCommentStartDelimiter), "blockCommentStartDelimiter must not be null or empty");
		checkArgument(StringUtils.isNotEmpty(blockCommentEndDelimiter), "blockCommentEndDelimiter must not be null or empty");

		StringBuilder sb = new StringBuilder();
		boolean inLiteral = false;
		boolean inEscape = false;
		char[] content = script.toCharArray();
		for (int i = 0; i < script.length(); i++) {
			char c = content[i];
			if (inEscape) {
				inEscape = false;
				sb.append(c);
				continue;
			}
			// MySQL style escapes
			if (c == '\\') {
				inEscape = true;
				sb.append(c);
				continue;
			}
			if (c == '\'') {
				inLiteral = !inLiteral;
			}
			if (!inLiteral) {
				if (script.startsWith(separator, i)) {
					// we've reached the end of the current statement
					if (sb.length() > 0) {
						statements.add(sb.toString());
						sb = new StringBuilder();
					}
					i += separator.length() - 1;
					continue;
				}
				else if (script.startsWith(commentPrefix, i)) {
					// skip over any content from the start of the comment to the EOL
					int indexOfNextNewline = script.indexOf("\n", i);
					if (indexOfNextNewline > i) {
						i = indexOfNextNewline;
						continue;
					}
					else {
						// if there's no EOL, we must be at the end
						// of the script, so stop here.
						break;
					}
				}
				else if (script.startsWith(blockCommentStartDelimiter, i)) {
					// skip over any block comments
					int indexOfCommentEnd = script.indexOf(blockCommentEndDelimiter, i);
					if (indexOfCommentEnd > i) {
						i = indexOfCommentEnd + blockCommentEndDelimiter.length() - 1;
						continue;
					}
					else {
						throw new ScriptParseException(String.format("Missing block comment end delimiter [%s].",
							blockCommentEndDelimiter), resource);
					}
				}
				else if (c == ' ' || c == '\n' || c == '\t') {
					// avoid multiple adjacent whitespace characters
					if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
						c = ' ';
					}
					else {
						continue;
					}
				}
			}
			sb.append(c);
		}
		if (StringUtils.isNotEmpty(sb.toString())) {
			statements.add(sb.toString());
		}
	}

	private static void checkArgument(boolean expression, String errorMessage) {
		if (!expression) {
			throw new IllegalArgumentException(errorMessage);
		}
	}

    /**
	 * Does the provided SQL script contain the specified delimiter?
	 * @param script the SQL script
	 * @param delim String delimiting each statement - typically a ';' character
	 */
	public static boolean containsSqlScriptDelimiters(String script, String delim) {
		boolean inLiteral = false;
		char[] content = script.toCharArray();
		for (int i = 0; i < script.length(); i++) {
			if (content[i] == '\'') {
				inLiteral = !inLiteral;
			}
			if (!inLiteral && script.startsWith(delim, i)) {
				return true;
			}
		}
		return false;
	}

    public static void executeDatabaseScript(DatabaseDelegate databaseDelegate, String scriptPath, String script) throws ScriptException {
        executeDatabaseScript(databaseDelegate, scriptPath, script, false, false, DEFAULT_COMMENT_PREFIX, DEFAULT_STATEMENT_SEPARATOR, DEFAULT_BLOCK_COMMENT_START_DELIMITER, DEFAULT_BLOCK_COMMENT_END_DELIMITER);
    }

    /**
	 * Execute the given database script.
	 * <p>Statement separators and comments will be removed before executing
	 * individual statements within the supplied script.
     * <p><b>Do not use this method to execute DDL if you expect rollback.</b>
     * @param databaseDelegate database delegate for script execution
     * @param scriptPath the resource (potentially associated with a specific encoding)
     * to load the SQL script from
     * @param script the raw script content
     *@param continueOnError whether or not to continue without throwing an exception
     * in the event of an error
     * @param ignoreFailedDrops whether or not to continue in the event of specifically
* an error on a {@code DROP} statement
     * @param commentPrefix the prefix that identifies comments in the SQL script &mdash;
* typically "--"
     * @param separator the script statement separator; defaults to
* {@value #DEFAULT_STATEMENT_SEPARATOR} if not specified and falls back to
* {@value #FALLBACK_STATEMENT_SEPARATOR} as a last resort
     * @param blockCommentStartDelimiter the <em>start</em> block comment delimiter; never
* {@code null} or empty
     * @param blockCommentEndDelimiter the <em>end</em> block comment delimiter; never
* {@code null} or empty       @throws ScriptException if an error occurred while executing the SQL script
	 */
	public static void executeDatabaseScript(DatabaseDelegate databaseDelegate, String scriptPath, String script, boolean continueOnError,
			boolean ignoreFailedDrops, String commentPrefix, String separator, String blockCommentStartDelimiter,
			String blockCommentEndDelimiter) throws ScriptException {

		try {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Executing database script from " + scriptPath);
			}

			long startTime = System.currentTimeMillis();
			List<String> statements = new LinkedList<>();

			if (separator == null) {
				separator = DEFAULT_STATEMENT_SEPARATOR;
			}
			if (!containsSqlScriptDelimiters(script, separator)) {
				separator = FALLBACK_STATEMENT_SEPARATOR;
			}

			splitSqlScript(scriptPath, script, separator, commentPrefix, blockCommentStartDelimiter,
				blockCommentEndDelimiter, statements);

			try (DatabaseDelegate closeableDelegate = databaseDelegate) {
				closeableDelegate.execute(statements, scriptPath, continueOnError, ignoreFailedDrops);
			}

			long elapsedTime = System.currentTimeMillis() - startTime;
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Executed database script from " + scriptPath + " in " + elapsedTime + " ms.");
			}
		}
		catch (Exception ex) {
			if (ex instanceof ScriptException) {
				throw (ScriptException) ex;
			}

			throw new UncategorizedScriptException(
				"Failed to execute database script from resource [" + script + "]", ex);
		}
	}

	public static class ScriptLoadException extends RuntimeException {
		public ScriptLoadException(String message) {
			super(message);
		}

		public ScriptLoadException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private static class ScriptParseException extends RuntimeException {
		public ScriptParseException(String format, String scriptPath) {
			super(String.format(format, scriptPath));
		}
	}

	public static class ScriptStatementFailedException extends RuntimeException {
		public ScriptStatementFailedException(String statement, int lineNumber, String scriptPath) {
			this(statement, lineNumber, scriptPath, null);
		}

		public ScriptStatementFailedException(String statement, int lineNumber, String scriptPath, Exception ex) {
			super(String.format("Script execution failed (%s:%d): %s", scriptPath, lineNumber, statement), ex);
		}
	}

    public static class UncategorizedScriptException extends RuntimeException {
        public UncategorizedScriptException(String s, Exception ex) {
            super(s, ex);
        }
    }
}
