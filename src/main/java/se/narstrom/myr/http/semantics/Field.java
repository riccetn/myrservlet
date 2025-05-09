package se.narstrom.myr.http.semantics;

import java.util.Objects;

public record Field(Token name, String value) {
	public Field(final Token name, final String value) {
		this.name = Objects.requireNonNull(name);
		this.value = validateValue(value);
	}

	private String validateValue(final String value) {
		if (value == null)
			throw new IllegalArgumentException("Field value cannot be null");
		// TODO: Validate field value
		return value;
	}

	public static Field parse(final String str) {
		final int colonIndex = str.indexOf(':');
		if (colonIndex == -1)
			throw new IllegalArgumentException("Invalid field: " + str);
		final Token name = new Token(str.substring(0, colonIndex).toLowerCase());
		final String value = str.substring(colonIndex + 1).trim();
		return new Field(name, value);
	}
}
