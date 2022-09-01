package com.example.targetapp.server_comm.exceptions;

public class EmptyMessageException extends Exception{
	public EmptyMessageException() {
		super();
	}

	public EmptyMessageException(String message) {
		super(message);
	}

	public EmptyMessageException(String message, Throwable cause) {
		super(message, cause);
	}

	public EmptyMessageException(Throwable cause) {
		super(cause);
	}

	protected EmptyMessageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
