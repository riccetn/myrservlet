package se.narstrom.myr.http.semantics;

import java.util.Objects;

// https://httpwg.org/specs/rfc9110.html#rfc.section.5.5
public record FieldValue(String value) {
	public FieldValue {
		validateValue(value);
	}

	@Override
	public final String toString() {
		return value;
	}

	private void validateValue(final String value) {
		Objects.requireNonNull(value);

		if(value.isEmpty())
			return;

		final char ch0 = value.charAt(0);
		final char chl = value.charAt(value.length() - 1);
		if (ch0 == 0x9 || ch0 == 0x20 || chl == 0x9 || chl == 0x20)
			throw new IllegalArgumentException();

		for (int i = 0; i < value.length(); ++i) {
			final char ch = value.charAt(i);
			if (ch < 0x9 || (0x9 < ch && ch < 0x20) || ch == 0x7F)
				throw new IllegalArgumentException();
		}
	}
}
