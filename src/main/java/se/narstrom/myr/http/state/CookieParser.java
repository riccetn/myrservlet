package se.narstrom.myr.http.state;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.Cookie;
import se.narstrom.myr.http.semantics.Token;

public final class CookieParser {
	// https://httpwg.org/specs/rfc6265.html#sane-cookie-syntax
	public static List<Cookie> parse(final String cookieString) {
		final List<Cookie> cookies = new ArrayList<>();

		final String[] cookiePairs = cookieString.split(";", -1);
		for (final String cookiePair : cookiePairs) {
			final int eq = cookiePair.indexOf('=');
			if (eq == -1)
				throw new IllegalArgumentException("Not a valid cookie string");

			final String cookieName = cookiePair.substring(0, eq);
			final String cookieValue = cookiePair.substring(eq + 1);

			cookies.add(new Cookie(Token.verifyToken(cookieName), verifyCookieValue(cookieValue)));
		}
		return List.copyOf(cookies);
	}

	public static String verifyCookieValue(final String cookieValue) {
		if (!isCookieValue(cookieValue))
			throw new IllegalArgumentException("Not a valid cookie");
		return cookieValue;
	}

	public static boolean isCookieValue(final String cookieValue) {
		int start = 0;
		int end = cookieValue.length();
		if (cookieValue.charAt(0) == '\"' && cookieValue.charAt(end - 1) == '\"') {
			start += 1;
			end -= 1;
		}
		for (int i = start; i < end; ++i) {
			if (!isCookieOctet(cookieValue.charAt(i)))
				return false;
		}
		return true;
	}

	public static boolean isCookieOctet(final char ch) {
		return ch == 0x21 || (0x23 <= ch && ch <= 0x2B) || (0x2D < ch && ch < 0x3A) || (0x3C < ch && ch < 0x5B) || (0x5D < ch && ch < 0x7E);
	}
}
