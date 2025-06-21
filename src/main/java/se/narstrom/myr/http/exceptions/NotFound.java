package se.narstrom.myr.http.exceptions;

import java.io.Serial;

public final class NotFound extends ClientError {
	@Serial
	private static final long serialVersionUID = 1L;

	public NotFound(final String message) {
		super(404, message);
	}

	public NotFound(final String message, final Throwable cause) {
		super(404, message, cause);
	}

}
