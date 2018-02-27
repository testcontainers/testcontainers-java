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

package org.testcontainers.jdbc.ext;

import org.testcontainers.jdbc.ContainerLessJdbcDelegate;

import javax.script.ScriptException;
import java.sql.Connection;
import java.util.List;

/**
 * Wrapper for database-agnostic ScriptUtils
 *
 * @see org.testcontainers.ext.ScriptUtils
 * @deprecated Needed only to keep binary compatibility for this internal API. Consider using database-agnostic ScriptUtils
 */
public abstract class ScriptUtils {

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
     * @see org.testcontainers.ext.ScriptUtils
     * @deprecated Needed only to keep binary compatibility for this internal API. Consider using database-agnostic ScriptUtils
     */
    public static void splitSqlScript(String resource, String script, String separator, String commentPrefix,
                                      String blockCommentStartDelimiter, String blockCommentEndDelimiter, List<String> statements) {
        org.testcontainers.ext.ScriptUtils.splitSqlScript(resource, script, separator, commentPrefix, blockCommentStartDelimiter, blockCommentEndDelimiter, statements);
    }

    /**
     * @see org.testcontainers.ext.ScriptUtils
     * @deprecated Needed only to keep binary compatibility for this internal API. Consider using database-agnostic ScriptUtils
     */
    public static boolean containsSqlScriptDelimiters(String script, String delim) {
        return org.testcontainers.ext.ScriptUtils.containsSqlScriptDelimiters(script, delim);
    }

    /**
     * @see org.testcontainers.ext.ScriptUtils
     * @deprecated Needed only to keep binary compatibility for this internal API. Consider using database-agnostic ScriptUtils
     */
    public static void executeSqlScript(Connection connection, String scriptPath, String script) throws ScriptException {
        org.testcontainers.ext.ScriptUtils.executeDatabaseScript(new ContainerLessJdbcDelegate(connection), scriptPath, script);
    }

    /**
     * @see org.testcontainers.ext.ScriptUtils
     * @deprecated Needed only to keep binary compatibility for this internal API. Consider using database-agnostic ScriptUtils
     */
    public static void executeSqlScript(Connection connection, String scriptPath, String script, boolean continueOnError,
                                        boolean ignoreFailedDrops, String commentPrefix, String separator, String blockCommentStartDelimiter,
                                        String blockCommentEndDelimiter) throws ScriptException {
        org.testcontainers.ext.ScriptUtils.executeDatabaseScript(new ContainerLessJdbcDelegate(connection), scriptPath,
                script, continueOnError, ignoreFailedDrops, commentPrefix, separator, blockCommentStartDelimiter, blockCommentEndDelimiter);
    }
}