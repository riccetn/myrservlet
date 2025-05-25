package se.narstrom.myr.http.semantics;

import static se.narstrom.myr.AugmentedBackusNaurFormUtils.isAlpha;
import static se.narstrom.myr.AugmentedBackusNaurFormUtils.isDigit;

// HTTP Semantics (RFC 9110) - 5.6.2 Tokens
public record Token(String value) {
	public Token(final String value) {
		this.value = verifyToken(value);
	}

	@Override
	public final String toString() {
		return value;
	}

	public static String verifyToken(final String str) {
		if (!isToken(str))
			throw new IllegalArgumentException("Invalid token: " + str);
		return str;
	}

	public static boolean isToken(final String str) {
		for (int i = 0; i < str.length(); ++i) {
			if (!isTokenChar(str.charAt(i)))
				return false;
		}
		return true;
	}

	public static boolean isTokenChar(final char c) {
		return c == '!' || c == '#' || c == '$' || c == '%' || c == '&' || c == '\'' || c == '*' || c == '+' || c == '-' || c == '.' || c == '^' || c == '_' || c == '`' || c == '|' || c == '~'
				|| isDigit(c) || isAlpha(c);
	}
}
