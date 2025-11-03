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
		this(value.charAt(0) == ':', new Token((value.charAt(0) == ':') ? value.substring(1) : value));
	}

	/* Java 25
	public FieldName(final String value) {
		if(value.charAt(0) == ':')
			this(true, new Token(value.substring(1)));
		else
			this(false, new Token(value));
	}
	*/

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
