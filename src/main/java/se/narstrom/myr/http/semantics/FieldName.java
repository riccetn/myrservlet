package se.narstrom.myr.http.semantics;

public record FieldName(Token value) {
	public FieldName(final Token value) {
		this.value = new Token(value.toString());
	}

	public FieldName(final String value) {
		this(new Token(value));
	}

	@Override
	public final String toString() {
		return value.toString();
	}

	@Override
	public final boolean equals(final Object other) {
		return other instanceof FieldName otherName && this.toString().equalsIgnoreCase(otherName.toString());
	}

	@Override
	public final int hashCode() {
		return toString().toLowerCase().hashCode();
	}
}
