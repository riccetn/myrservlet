package se.narstrom.myr.servlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import se.narstrom.myr.http.exceptions.BadRequest;
import se.narstrom.myr.http.exceptions.HttpStatusCodeException;
import se.narstrom.myr.uri.Query;
import se.narstrom.myr.uri.UrlEncoding;

public record CanonicalizedUriPath(List<CanonicalizedSegment> segments, Query query) {
	public CanonicalizedUriPath {
		segments = List.copyOf(segments);
		validateSegments(segments);
		Objects.requireNonNull(query);
	}

	private static void validateSegments(final List<CanonicalizedSegment> segments) {
		if (segments.isEmpty())
			throw new BadRequest("Empty segment list");
		for (final CanonicalizedSegment segment : segments.subList(0, segments.size() - 1)) {
			if (segment.name().isEmpty())
				throw new BadRequest("Empty path segment");
		}
	}

	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#uri-path-canonicalization
	public static CanonicalizedUriPath canonicalize(final String uri) throws HttpStatusCodeException {
		// 1. Discard fragment
		final int hash = uri.indexOf('#');
		if (hash != -1)
			throw new BadRequest("URI with fragment");

		// 2. Separation of path and query
		final int question = uri.indexOf('?');
		final String path;
		final Query query;
		if (question != -1) {
			path = uri.substring(0, question);
			query = new Query(uri.substring(question + 1));
		} else {
			path = uri;
			query = new Query("");
		}

		// 3. Split into segments
		if (path.isEmpty() || path.charAt(0) != '/')
			throw new BadRequest("URI path not beginning with /");
		final List<String> segments = List.of(path.substring(1).split("/", -1));

		final List<CanonicalizedSegment> canonicalizedSegments = new ArrayList<>();
		for (int i = 0; i < segments.size(); ++i) {
			final String segment = segments.get(i);

			// 4. remote path parameters
			final int semi = segment.indexOf(';');
			final String segmentName;
			final String segmentParameters;
			if (semi != -1) {
				segmentName = segment.substring(0, semi);
				segmentParameters = segment.substring(semi + 1);
			} else {
				segmentName = segment;
				segmentParameters = "";
			}

			// 5. Decode
			final String decodedSegmentName;
			final String decodedSegmentParameters;
			try {
				decodedSegmentName = UrlEncoding.percentDecode(segmentName);
				decodedSegmentParameters = UrlEncoding.percentDecode(segmentParameters);
			} catch (final IllegalArgumentException ex) {
				throw new BadRequest("Invalid percent encoding", ex);
			}

			// 6. Remove empty segments
			if (i != segments.size() - 1 && decodedSegmentName.isEmpty()) {
				if (semi != -1)
					throw new BadRequest("Empty segment with parameter");
				continue;
			}

			// 7. Remove dot segments
			if (decodedSegmentName.equals(".")) {
				if (semi != -1)
					throw new BadRequest("'.' segment with parameter");
				if (!segmentParameters.isEmpty())
					throw new BadRequest("Non-empty parameters on '.' segment");
				if (!decodedSegmentName.equals(segmentName))
					throw new BadRequest("Encoded '.' in '.' segment");
				continue;
			}
			if (decodedSegmentName.equals("..")) {
				if (semi != -1)
					throw new BadRequest("'..' segment with parameter");
				if (canonicalizedSegments.isEmpty())
					throw new BadRequest("Attempt to .. out of webroot");
				if (!decodedSegmentName.equals(segmentName))
					throw new BadRequest("Encoded '.' in '..' segment");
				canonicalizedSegments.removeLast();
				continue;
			}

			canonicalizedSegments.add(new CanonicalizedSegment(decodedSegmentName, decodedSegmentParameters));
		}

		if (canonicalizedSegments.isEmpty())
			canonicalizedSegments.add(new CanonicalizedSegment("", ""));

		return new CanonicalizedUriPath(canonicalizedSegments, query);
	}

	@Override
	public final String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final CanonicalizedSegment segment : segments) {
			sb.append("/").append(segment.name());
		}
		return sb.toString();
	}
}
