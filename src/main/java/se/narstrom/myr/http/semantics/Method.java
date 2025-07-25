package se.narstrom.myr.http.semantics;

// HTTP Semantics (RFC 9110) - 9. Methods
public record Method(Token token) {
	public static Method parse(final String str) {
		return new Method(new Token(str));
	}

	@Override
	public final String toString() {
		return token.toString();
	}
}
