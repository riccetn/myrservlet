package se.narstrom.myr.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import se.narstrom.myr.http.v1.RequestTarget;

final class CanonicalizedPathTests {
	static Stream<Arguments> successfulPathSource() {
		// @formatter:off
		return Stream.of(
				Arguments.of("/foo/bar", "/foo/bar"),
				Arguments.of("/foo/bar;jsessionid=1234", "/foo/bar"),
				Arguments.of("/foo/bar/", "/foo/bar/"),
				Arguments.of("/foo/bar/;jsessionid=1234", "/foo/bar/"),
				Arguments.of("/foo;/bar;", "/foo/bar"),
				Arguments.of("/foo;/bar;/;", "/foo/bar/"),
				Arguments.of("/foo/b%25r", "/foo/b%r"),
				Arguments.of("/foo/./bar", "/foo/bar"),
				Arguments.of("/foo/././bar", "/foo/bar"),
				Arguments.of("/./foo/bar", "/foo/bar"),
				Arguments.of("/foo/bar/.", "/foo/bar"),
				Arguments.of("/foo/bar/./", "/foo/bar/"),
				Arguments.of("/foo/bar/./;", "/foo/bar/"),
				Arguments.of("/foo/.bar", "/foo/.bar"),
				Arguments.of("/foo/../bar", "/bar"),
				Arguments.of("/foo/./../bar", "/bar"),
				Arguments.of("/foo/bar/..", "/foo"),
				Arguments.of("/foo/bar/../", "/foo/"),
				Arguments.of("/foo/bar/../;", "/foo/"),
				Arguments.of("/foo/..bar", "/foo/..bar"),
				Arguments.of("/foo/.../bar", "/foo/.../bar"),
				Arguments.of("/foo//bar", "/foo/bar"),
				Arguments.of("//foo//bar//", "/foo/bar/"),
				Arguments.of("/foo//../bar", "/bar"),
				Arguments.of("/foo%E2%82%ACbar", "/foo€bar"),
				Arguments.of("/foo%20bar", "/foo bar"),
				Arguments.of("/foo/bar?q", "/foo/bar"),
				Arguments.of("/foo/bar/?q", "/foo/bar/"),
				Arguments.of("/foo/bar;?q", "/foo/bar"),
				Arguments.of("/", "/"),
				Arguments.of("//", "/"),
				Arguments.of("/.", "/"),
				Arguments.of("/./", "/"),
				Arguments.of("/?q", "/"));
		// @formatter:on
	}

	@ParameterizedTest
	@MethodSource("successfulPathSource")
	void successfulPathTests(final String path, final String expectedCanonicalizedPath) {
		final RequestTarget requestTarget = RequestTarget.parse(path);
		final CanonicalizedPath canonicalizedPath = CanonicalizedPath.canonicalize(requestTarget.absolutePath());
		assertEquals(expectedCanonicalizedPath, canonicalizedPath.toString());
	}

	static Stream<String> failurePathSource() {
		// @formatter:off
		return Stream.of(
				"foo/bar",
				"/foo%00/bar/",
				"/foo%7Fbar",
				"/foo%2Fbar",
				"/foo%2Fb%25r",
				"/foo\\bar",
				"/foo%5Cbar",
				"/foo;%2F/bar",
				"/foo/%2e/bar",
				"/foo/.;/bar",
				"/foo/%2e;/bar",
				"/foo/.%2Fbar",
				"/foo/.%5Cbar",
				"/foo/bar/.;",
				"/foo/../../bar",
				"/../foo/bar",
				"/foo/%2e%2E/bar",
				"/foo/%2e%2e/%2E%2E/bar",
				"/foo/..;/bar",
				"/foo/%2e%2E;/bar",
				"/foo/..%2Fbar",
				"/foo/..%5Cbar",
				"/foo/bar/..;",
				"/;/foo;/;/bar/;/;",
				"/foo/;/../bar",
				"/foo%E2%82",
				"/foo%E2%82bar",
				"/foo%-1/bar",
				"/foo%XX/bar",
				"/foo%/bar",
				"/foo/bar%0",
				"/good%20/bad%/%20mix%",
				"/foo/bar#f",
				"/foo/bar?q#f",
				"/foo/bar/#f",
				"/foo/bar/?q#f",
				"/foo/bar;#f",
				"/foo/bar;?q#f",
				"/;/",
				"/..",
				"/../",
				"foo/bar/",
				"./foo/bar/",
				"%2e/foo/bar/",
				"../foo/bar/",
				".%2e/foo/bar/",
				";/foo/bar/",
				"/#f",
				"#f",
				"?q");
		// @formatter:on
	}

	@ParameterizedTest
	@MethodSource("failurePathSource")
	void failurePathTests(final String path) {
		assertThrows(IllegalArgumentException.class, () -> {
			final RequestTarget requestTarget = RequestTarget.parse(path);
			CanonicalizedPath.canonicalize(requestTarget.absolutePath());
		});
	}
}
