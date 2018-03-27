package org.testcontainers.exception;

public class RuntimeSqlException extends RuntimeException {

	public RuntimeSqlException() {
		
	}

	public RuntimeSqlException(String message) {
		super(message);
	}

	public RuntimeSqlException(Throwable cause) {
		super(cause);
	}

	public RuntimeSqlException(String message, Throwable cause) {
		super(message, cause);
	}

	public RuntimeSqlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
