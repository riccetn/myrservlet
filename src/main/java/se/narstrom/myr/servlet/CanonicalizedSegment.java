package se.narstrom.myr.servlet;

import java.util.Objects;

public record CanonicalizedSegment(String name, String parameters) {
	public CanonicalizedSegment {
		validateName(name);
		validateParameter(parameters);
	}

	private static void validateName(final String name) {
		Objects.requireNonNull(name);
		if (name.equals(".") || name.equals(".."))
			throw new IllegalArgumentException();
		for (int i = 0; i < name.length(); ++i) {
			final char ch = name.charAt(i);
			if (ch < 0x20 || ch == '/' || ch == '\\' || ch == ';' || ch == 0x7F)
				throw new IllegalArgumentException();
		}
	}

	private static void validateParameter(final String parameters) {
		Objects.requireNonNull(parameters);
		for (int i = 0; i < parameters.length(); ++i) {
			final char ch = parameters.charAt(i);
			if (ch < 0x20 || ch == '/' || ch == '\\' || ch == 0x7F)
				throw new IllegalArgumentException();
		}
	}
}
