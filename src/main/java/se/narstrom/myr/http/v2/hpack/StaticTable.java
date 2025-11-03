package se.narstrom.myr.http.v2.hpack;

import java.util.List;

import se.narstrom.myr.http.semantics.Field;

public final class StaticTable {
	private static final List<Field> TABLE = List.of(
			Field.of(":authority", ""),
			Field.of(":method", "GET"),
			Field.of(":method", "POST"),
			Field.of(":path", "/"),
			Field.of(":path", "/index.html"),
			Field.of(":scheme", "http"),
			Field.of(":scheme", "https"),
			Field.of(":status", "200"),
			Field.of(":status", "204"),
			Field.of(":status", "206"),
			Field.of(":status", "304"),
			Field.of(":status", "400"),
			Field.of(":status", "404"),
			Field.of(":status", "500"),
			Field.of("accept-charset", ""),
			Field.of("accept-encoding", "gzip,deflate"),
			Field.of("accept-language", ""),
			Field.of("accept-ranges", ""),
			Field.of("accept", ""),
			Field.of("access-control-allow-origin", ""),
			Field.of("age", ""),
			Field.of("allow", ""),
			Field.of("authorization", ""),
			Field.of("cache-control", ""),
			Field.of("content-disposition", ""),
			Field.of("content-encoding", ""),
			Field.of("content-language", ""),
			Field.of("content-length", ""),
			Field.of("content-location", ""),
			Field.of("content-range", ""),
			Field.of("content-type", ""),
			Field.of("cookie", ""),
			Field.of("date", ""),
			Field.of("etag", ""),
			Field.of("expect", ""),
			Field.of("expires", ""),
			Field.of("from", ""),
			Field.of("host", ""),
			Field.of("if-match", ""),
			Field.of("if-modified-since", ""),
			Field.of("if-none-match", ""),
			Field.of("if-range", ""),
			Field.of("if-unmodified-since", ""),
			Field.of("last-modified", ""),
			Field.of("link", ""),
			Field.of("location", ""),
			Field.of("max-forwards", ""),
			Field.of("proxy-authenticate", ""),
			Field.of("proxy-authorization", ""),
			Field.of("range", ""),
			Field.of("referer", ""),
			Field.of("refresh", ""),
			Field.of("retry-after", ""),
			Field.of("server", ""),
			Field.of("set-cookie", ""),
			Field.of("strict-transport-security", ""),
			Field.of("transfer-encoding", ""),
			Field.of("user-agent", ""),
			Field.of("vary", ""),
			Field.of("via", ""),
			Field.of("www-authenticate", "")
			);

	public static Field get(final int index) {
		return TABLE.get(index-1);
	}

	public static int size() {
		return TABLE.size();
	}
	
	public static void main(final String[] args) {
		System.out.println("LOL");
	}
}
