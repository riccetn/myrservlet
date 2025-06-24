package se.narstrom.myr.http.exceptions;

import java.io.Serial;

public abstract class ServerError extends HttpStatusCodeException {
	@Serial
	private static final long serialVersionUID = 1L;

	public ServerError(final int statusCode, final String message) {
		super(statusCode, message);
		if (statusCode / 100 != 5)
			throw new IllegalArgumentException("Invalid status code: " + statusCode);
	}

	public ServerError(final int statusCode, final String message, final Throwable cause) {
		super(statusCode, message, cause);
		if (statusCode / 100 != 5)
			throw new IllegalArgumentException("Invalid status code: " + statusCode);
	}
}
