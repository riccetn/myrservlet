package se.narstrom.myr.http.exceptions;

import java.io.Serial;

public abstract class ClientError extends HttpStatusCodeException {
	@Serial
	private static final long serialVersionUID = 1L;

	protected ClientError(final int statusCode, final String message) {
		super(statusCode, message);
	}

	protected ClientError(final int statusCode, final String message, final Throwable cause) {
		super(statusCode, message, cause);
		if (statusCode / 100 != 4)
			throw new IllegalArgumentException("Invalid status code: " + statusCode);
	}
}
