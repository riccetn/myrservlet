package se.narstrom.myr.http.semantics;

public record FieldName(boolean pseudo, Token value) {
	public FieldName(final Token value) {
		this(false, value);
	}

	public FieldName(final boolean pseudo, final Token value) {
		this.pseudo = pseudo;
		this.value = new Token(value.toString());
	}

	public FieldName(final String value) {
		final boolean psedo;
		final Token token;
		if(value.charAt(0) == ':') {
			psedo = true;
			token = new Token(value.substring(1));
		} else {
			psedo = false;
			token = new Token(value);
		}
		this(psedo, token);
	}

	@Override
	public final String toString() {
		if(pseudo)
			return ":" + value.toString();
		else
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
