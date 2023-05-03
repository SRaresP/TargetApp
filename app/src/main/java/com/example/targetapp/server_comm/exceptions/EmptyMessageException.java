package com.example.targetapp.server_comm.exceptions;

import androidx.annotation.NonNull;

public class EmptyMessageException extends Exception{
	public EmptyMessageException() {
		super();
	}

	public EmptyMessageException(final @NonNull String message) {
		super(message);
	}

	public EmptyMessageException(final @NonNull String message, final @NonNull Throwable cause) {
		super(message, cause);
	}

	public EmptyMessageException(final @NonNull Throwable cause) {
		super(cause);
	}

	protected EmptyMessageException(final String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
