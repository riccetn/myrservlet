package se.narstrom.myr.uri;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// https://url.spec.whatwg.org/#application/x-www-form-urlencoded
public final class UrlEncoding {
	private UrlEncoding() {
	}

	public static Map<String, List<String>> parse(final String input) {
		final Map<String, List<String>> result = new HashMap<>();

		for (final String inputParam : input.split("&", -1)) {
			if (inputParam.isEmpty())
				continue;

			final int equalIndex = inputParam.indexOf('=');

			final String name;
			final String value;
			if (equalIndex == -1) {
				name = percentDecode(inputParam);
				value = "";
			} else {
				name = percentDecode(inputParam.substring(0, equalIndex));
				value = percentDecode(inputParam.substring(equalIndex + 1));
			}

			result.computeIfAbsent(name, _ -> new ArrayList<>()).add(value);
		}

		return result.entrySet().stream()
				.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, ent -> List.copyOf(ent.getValue())));
	}

	private static String percentDecode(final String input) {
		return URLDecoder.decode(input, StandardCharsets.UTF_8);
	}
}
