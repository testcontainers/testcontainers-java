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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
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

	public static void runSqlScriptPath(String sqlScriptPath, String delimiter, boolean continueOnError, Connection con) throws SQLException {
		BufferedReader reader = new BufferedReader(new
				InputStreamReader(ScriptUtils.class.getClassLoader().getResourceAsStream(sqlScriptPath)));
		runSql(reader, delimiter, continueOnError,con);
	}

	public static void runSqlScript(String sqlScript, String delimiter, boolean continueOnError, Connection con) throws SQLException {
		BufferedReader reader = new BufferedReader(new
				StringReader(sqlScript));
		runSql(reader, delimiter, continueOnError, con);
	}

	public static void runSql(Reader reader, String delimiter, boolean continueOnError, Connection con) throws SQLException {

		ScriptRunner scriptRunner = new ScriptRunner(con);
		if(delimiter!=null){
			scriptRunner.setDelimiter(delimiter);
		}
	
		scriptRunner.setStopOnError(!continueOnError);
		
		scriptRunner.setSendFullScript(true);
		scriptRunner.setAutoCommit(true);
		scriptRunner.runScript(reader);
	}

	public static void executeDatabaseScript(Connection connection, String scriptPath, String script) throws ScriptException {
		executeDatabaseScript(connection, scriptPath, script, false, null);
	}

	/**
	 * Execute the given database script.
	 * <p><b>Do not use this method to execute DDL if you expect rollback.</b>
	 * @param connection the connection to use to execute the script
	 * @param scriptPath the resource (potentially associated with a specific encoding)
	 * to load the SQL script from
	 * @param script the raw script content
	 * @param continueOnError whether or not to continue without throwing an exception
	 * in the event of an error
	 * @param separator the script statement separator; typically ';'

	 */
	public static void executeDatabaseScript(Connection connection, String scriptPath, String script, boolean continueOnError,
			 String delimiter) throws ScriptException {

		try {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Executing database script from " + scriptPath);
			}

			long startTime = System.currentTimeMillis();
			
			if(scriptPath!=null){
				runSqlScriptPath(scriptPath, delimiter, continueOnError,  connection);
			} else if(script!=null){
				runSqlScript(script, delimiter, continueOnError, connection);
			} 

			long elapsedTime = System.currentTimeMillis() - startTime;
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Executed database script from " + scriptPath + " in " + elapsedTime + " ms.");
			}
		}
		catch (Exception ex) {
			throw new UncategorizedScriptException(
					"Failed to execute database script from resource [" + script + "]", ex);
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
	
	public static class ScriptStatementFailedException extends RuntimeException {
		public ScriptStatementFailedException(String statement, int lineNumber, String scriptPath, Exception ex) {
			super(String.format("Script execution failed (%s:%d): %s", scriptPath, lineNumber, statement), ex);
		}
	}

	private static class UncategorizedScriptException extends RuntimeException {
		public UncategorizedScriptException(String s, Exception ex) {
			super(s, ex);
		}
	}
}
