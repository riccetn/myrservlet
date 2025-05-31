package se.narstrom.myr.http.semantics;

public record FieldName(Token value) {
	public FieldName(final Token value) {
		this.value = new Token(value.toString().toLowerCase());
	}

	public FieldName(final String value) {
		this(new Token(value));
	}

	@Override
	public final String toString() {
		return value.toString();
	}
}
