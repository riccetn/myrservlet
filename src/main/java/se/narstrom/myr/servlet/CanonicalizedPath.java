package se.narstrom.myr.servlet;

import java.util.ArrayList;
import java.util.List;

import se.narstrom.myr.http.v1.AbsolutePath;
import se.narstrom.myr.uri.Segment;
import se.narstrom.myr.uri.UrlEncoding;

public record CanonicalizedPath(List<CanonicalizedSegment> segments) {
	public CanonicalizedPath(final List<CanonicalizedSegment> segments) {
		this.segments = List.copyOf(segments);
		validateSegments(this.segments);
	}

	private void validateSegments(final List<CanonicalizedSegment> segments) {
		if (segments.isEmpty())
			throw new IllegalArgumentException();
		for (final CanonicalizedSegment segment : segments.subList(0, segments.size() - 1)) {
			if (segment.name().isEmpty())
				throw new IllegalArgumentException();
		}
	}

	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#uri-path-canonicalization
	public static CanonicalizedPath canonicalize(final AbsolutePath uriPath) {
		// 1. Discard fragment (completed in HTTP layer)
		// 2. Separation of path and query (completed in HTTP layer)
		// 3. Split into segments (completed in HTTP layer)

		final List<Segment> segments = uriPath.segments();
		final List<CanonicalizedSegment> canonicalizedSegments = new ArrayList<>();
		for (int i = 0; i < segments.size(); ++i) {
			final Segment segment = segments.get(i);

			// 4. remote path parameters
			final String segmentValue = segment.value();
			final int semi = segmentValue.indexOf(';');
			final String segmentName;
			final String segmentParameters;
			if (semi != -1) {
				segmentName = segmentValue.substring(0, semi);
				segmentParameters = segmentValue.substring(semi+1);
			} else {
				segmentName = segmentValue;
				segmentParameters = "";
			}

			// 5. Decode
			final String decodedSegmentName = UrlEncoding.percentDecode(segmentName);
			final String decodedSegmentParameters = UrlEncoding.percentDecode(segmentParameters);

			// 6. Remove empty segments
			if (i != uriPath.segments().size() - 1 && decodedSegmentName.isEmpty()) {
				if (semi != -1)
					throw new IllegalArgumentException("Empty segment with parameter");
				continue;
			}

			// 7. Remove dot segments
			if (decodedSegmentName.equals(".")) {
				if (semi != -1)
					throw new IllegalArgumentException("'.' segment with parameter");
				if (!segmentParameters.isEmpty())
					throw new IllegalArgumentException("Non-empty parameters on '.' segment");
				if (!decodedSegmentName.equals(segmentName))
					throw new IllegalArgumentException("Encoded '.' in '.' segment");
				continue;
			}
			if (decodedSegmentName.equals("..")) {
				if (semi != -1)
					throw new IllegalArgumentException("'..' segment with parameter");
				if (canonicalizedSegments.isEmpty())
					throw new IllegalArgumentException("Attempt to .. out of webroot");
				if (!decodedSegmentName.equals(segmentName))
					throw new IllegalArgumentException("Encoded '.' in '..' segment");
				canonicalizedSegments.removeLast();
				continue;
			}

			canonicalizedSegments.add(new CanonicalizedSegment(decodedSegmentName, decodedSegmentParameters));
		}

		if (canonicalizedSegments.isEmpty())
			canonicalizedSegments.add(new CanonicalizedSegment("", ""));

		return new CanonicalizedPath(canonicalizedSegments);
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
