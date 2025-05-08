package se.narstrom.myr.http.v1;

import se.narstrom.myr.http.semantics.Method;

public record RequestLine(Method method, RequestTarget target, HttpVersion version) {

	public static RequestLine parse(final String requestLine) {
		final String[] parts = requestLine.split(" ");
		if (parts.length != 3)
			throw new IllegalArgumentException("Invalid request line: " + requestLine);
		return new RequestLine(Method.parse(parts[0]), RequestTarget.parse(parts[1]), HttpVersion.parse(parts[2]));
	}
}
