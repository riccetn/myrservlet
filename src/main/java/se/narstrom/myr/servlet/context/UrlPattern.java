package se.narstrom.myr.servlet.context;

import jakarta.servlet.http.MappingMatch;

sealed interface UrlPattern {
	MappingMatch type();

	String pattern();

	public static UrlPattern parse(final String pattern) {
		// 12.2. Specification of Mappings
		// ===============================
		// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#specification-of-mappings

		// - A string beginning with a "/" character and ending with a "/*" suffix is used for path mapping.
		if (pattern.startsWith("/") && pattern.endsWith("/*")) {
			final String path = pattern.substring(0, pattern.length() - 2);
			return new UrlPattern.Path(path);
		}

		// - A string beginning with a "*." prefix is used as an extension mapping.
		if (pattern.startsWith("*.")) {
			final String extention = pattern.substring(2);
			return new UrlPattern.Extension(extention);
		}

		// - The empty string ("") is a special URL pattern that exactly maps to the application’s context root
		if (pattern.equals("")) {
			return new UrlPattern.ContextRoot();
		}

		// - A string containing only the "/" character indicates the "default" servlet of the application
		if (pattern.equals("/")) {
			return new UrlPattern.Default();
		}

		// - All other strings are used for exact matches only
		return new UrlPattern.Exact(pattern);
	}

	record ContextRoot() implements UrlPattern {
		@Override
		public MappingMatch type() {
			return MappingMatch.CONTEXT_ROOT;
		}

		@Override
		public String pattern() {
			return "";
		}
	}

	record Default() implements UrlPattern {
		@Override
		public MappingMatch type() {
			return MappingMatch.DEFAULT;
		}

		@Override
		public String pattern() {
			return "/";
		}
	}

	record Exact(String uri) implements UrlPattern {
		@Override
		public MappingMatch type() {
			return MappingMatch.EXACT;
		}

		@Override
		public String pattern() {
			return uri;
		}
	}

	record Extension(String extension) implements UrlPattern {
		@Override
		public MappingMatch type() {
			return MappingMatch.EXTENSION;
		}

		@Override
		public String pattern() {
			return "*." + extension;
		}
	}

	record Path(String path) implements UrlPattern {
		public MappingMatch type() {
			return MappingMatch.PATH;
		}

		@Override
		public String pattern() {
			return path + "/*";
		}
	}
}
