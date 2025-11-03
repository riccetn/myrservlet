package se.narstrom.myr.http.semantics;

import java.util.Objects;

public record Field(FieldName name, FieldValue value) {
	public Field {
		Objects.requireNonNull(name);
		Objects.requireNonNull(value);
	}

	public static Field parse(final String str) {
		final int colonIndex = str.indexOf(':');
		if (colonIndex == -1)
			throw new IllegalArgumentException("Invalid field: " + str);
		final FieldName name = new FieldName(str.substring(0, colonIndex));
		final FieldValue value = new FieldValue(str.substring(colonIndex + 1).trim());
		return new Field(name, value);
	}
	
	public static Field of(final String name, final String value) {
		return new Field(new FieldName(name), new FieldValue(value));
	}
}
