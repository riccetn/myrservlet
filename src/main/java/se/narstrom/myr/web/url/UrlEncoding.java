package se.narstrom.myr.web.url;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// https://url.spec.whatwg.org/#application/x-www-form-urlencoded
public final class UrlEncoding {
	private UrlEncoding() {
	}

	public static Map<String, List<String>> parseToMap(final String input) {
		final List<Map.Entry<String, String>> entries = parse(input);
		final HashMap<String, List<String>> map = HashMap.newHashMap(entries.size());

		for (final Map.Entry<String, String> entry : entries) {
			final List<String> list = map.computeIfAbsent(entry.getKey(), _ -> new ArrayList<>());
			list.add(entry.getValue());
		}

		for (final Map.Entry<String, List<String>> entry : map.entrySet()) {
			entry.setValue(List.copyOf(entry.getValue()));
		}

		return Map.copyOf(map);
	}

	public static List<Map.Entry<String, String>> parse(final String input) {
		// 1. Let sequences be the result of splitting input on 0x26 (&).
		final String[] sequences = input.split("&", -1);

		// 2. Let output be an initially empty list of name-value tuples where both name
		// and value hold a string.
		final List<Map.Entry<String, String>> output = new ArrayList<>(sequences.length);

		// 3. For each byte sequence bytes in sequences:
		for (final String bytes : sequences) {
			// 1. If bytes is the empty byte sequence, then continue.
			if (bytes.isEmpty())
				continue;

			final int equalIndex = bytes.indexOf('=');

			String name;
			String value;

			// 2. If bytes contains a 0x3D (=) ...
			if (equalIndex != -1) {
				// ... then let name be the bytes from the start of bytes up to but excluding
				// its first 0x3D (=), ...
				name = bytes.substring(0, equalIndex);
				// ... and let value be the bytes, if any, after the first 0x3D (=) up to the
				// end of bytes.
				value = bytes.substring(equalIndex + 1);
			} else {
				// 3. Otherwise, let name have the value of bytes and let value be the empty
				// byte sequence.
				name = bytes;
				value = "";
			}

			// 4. Replace any 0x2B (+) in name and value with 0x20 (SP).
			name = name.replace('+', ' ');
			value = value.replace('+', ' ');

			// 5. Let nameString and valueString be the result of running UTF-8 decode
			// without BOM on the percent-decoding of name and value, respectively.
			final String nameString = PercentEncoded.percentDecodeToString(name);
			final String valueString = PercentEncoded.percentDecodeToString(value);

			// 6. Append (nameString, valueString) to output.
			output.add(Map.entry(nameString, valueString));
		}

		// 4. Return output.
		return List.copyOf(output);
	}
}
