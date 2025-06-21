package se.narstrom.myr.http.exceptions;

import java.io.Serial;

public final class BadRequest extends ClientError {
	@Serial
	private static final long serialVersionUID = 1L;

	public BadRequest(String message) {
		super(400, message);
	}

	public BadRequest(String message, Throwable cause) {
		super(400, message, cause);
	}
}
