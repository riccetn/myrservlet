package se.narstrom.myr.uri;

public record Query(String value) {
	public Query {
		if (!isQuery(value))
			throw new IllegalArgumentException("Invalid query: " + value);
	}

	public static boolean isQuery(final String query) {
		for (int i = 0; i < query.length(); ++i) {
			char ch = query.charAt(i);
			if (!UriUtils.isPathChar(ch) && ch != '/' && ch != '?') {
				return false;
			}
		}
		return true;
	}

	@Override
	public final String toString() {
		return value;
	}
}
