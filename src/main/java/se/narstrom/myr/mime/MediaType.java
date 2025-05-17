package se.narstrom.myr.mime;

import java.util.HashMap;
import java.util.Map;

public record MediaType(String type, String subtype, Map<String, String> parameters) {
	public MediaType(final String type, final String subtype, final Map<String, String> parameters) {
		this.type = validateType(type);
		this.subtype = validateType(subtype);
		this.parameters = validateParameters(parameters);
	}

	public String render() {
		final StringBuilder sb = new StringBuilder();
		sb.append(type);
		sb.append("/");
		sb.append(subtype);
		for (final Map.Entry<String, String> param : parameters.entrySet()) {
			sb.append(";");
			sb.append(param.getKey());
			sb.append("=");
			maybeQuote(sb, param.getValue());
		}
		return sb.toString();
	}

	// https://datatracker.ietf.org/doc/html/rfc2045#section-5.1
	public static MediaType parse(final String input) {
		char ch = '\0';
		int i = 0;
		final StringBuilder type = new StringBuilder();
		final StringBuilder subtype = new StringBuilder();

		for (; i < input.length(); ++i) {
			ch = input.charAt(i);
			if (!isTokenChar(ch))
				break;
			type.append(ch);
		}

		if (type.isEmpty())
			throw new IllegalArgumentException("Expected token");

		if (ch != '/')
			throw new IllegalArgumentException("Expected '/'");
		++i;

		for (; i < input.length(); ++i) {
			ch = input.charAt(i);
			if (!isTokenChar(ch))
				break;
			subtype.append(ch);
		}

		if (subtype.isEmpty())
			throw new IllegalArgumentException("Expected token");

		final Map<String, String> parameters = new HashMap<>();

		while (i != input.length()) {
			final StringBuilder name = new StringBuilder();
			final StringBuilder value = new StringBuilder();

			for (; i < input.length(); ++i) {
				ch = input.charAt(i);
				if (ch != ' ' && ch != '\t')
					break;
			}

			if (ch != ';')
				throw new IllegalArgumentException("Expected ';'");
			++i;

			for (; i < input.length(); ++i) {
				ch = input.charAt(i);
				if (ch != ' ' && ch != '\t')
					break;
			}

			for (; i < input.length(); ++i) {
				ch = input.charAt(i);
				if (!isTokenChar(ch))
					break;
				name.append(ch);
			}

			if (name.isEmpty())
				throw new IllegalArgumentException("Expected token");

			if (ch != '=')
				throw new IllegalArgumentException("Expected '='");
			++i;

			boolean quote = false;
			for (; i < input.length(); ++i) {
				ch = input.charAt(i);
				if (quote) {
					switch (ch) {
						case '\\' -> {
							if (i == input.length() - 1)
								throw new IllegalArgumentException("unfinished quoted-pair at end of input");
							ch = input.charAt(i++);
							value.append(ch);
						}
						case '\"' -> quote = false;
						default -> value.append(ch);
					}
				} else {
					if (ch == '"')
						quote = true;
					else if (isTokenChar(ch))
						value.append(ch);
					else
						break;
				}
			}

			if (quote)
				throw new IllegalArgumentException("Unclosed quoted-string");

			if (value.isEmpty())
				throw new IllegalArgumentException("Empty parameter value");

			parameters.put(name.toString(), value.toString());
		}

		return new MediaType(type.toString(), subtype.toString(), parameters);
	}

	private static String validateType(final String type) {
		final String token = validateToken(type);
		return token.toLowerCase();
	}

	private static Map<String, String> validateParameters(final Map<String, String> parameters) {
		@SuppressWarnings("unchecked")
		final Map.Entry<String, String>[] entries = new Map.Entry[parameters.size()];
		int i = 0;
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			final String key = validateToken(entry.getKey()).toLowerCase();
			final String value = validateParameterValue(entry.getValue());
			entries[i++] = Map.entry(key, value);
		}
		return Map.ofEntries(entries);
	}

	private static String validateToken(final String token) {
		if (!isToken(token))
			throw new IllegalArgumentException("Not a token " + token);
		return token;
	}

	private static final String validateParameterValue(final String value) {
		for (int i = 0; i < value.length(); ++i) {
			char ch = value.charAt(i);
			if (ch < 0x20 || ch == 0x7F)
				throw new IllegalArgumentException("Parameter value cannot contain control characters");
		}
		return value;
	}

	private static boolean isToken(final String token) {
		for (int i = 0; i < token.length(); ++i)
			if (!isTokenChar(token.charAt(i)))
				return false;
		return true;
	}

	private static boolean isTokenChar(final char tchar) {
		return tchar == '!' || tchar == '#' || tchar == '$' || tchar == '%' || tchar == '&' || tchar == '\'' || tchar == '*' || tchar == '+' || tchar == '-' || tchar == '.' || tchar == '^'
				|| tchar == '_' || tchar == '`' || tchar == '|' || tchar == '~' || (tchar >= '0' && tchar <= '9') || (tchar >= 'A' && tchar <= 'Z') || (tchar >= 'a' && tchar <= 'z');
	}

	private static final void maybeQuote(final StringBuilder sb, final String value) {
		if (isToken(value))
			sb.append(value);
		else
			quote(sb, value);
	}

	private static final void quote(final StringBuilder sb, final String value) {
		sb.append("\"");
		for (char ch : value.toCharArray()) {
			if (ch == '\"' || ch == '\\')
				sb.append('\\');
			sb.append(ch);
		}
		sb.append("\"");
	}
}
