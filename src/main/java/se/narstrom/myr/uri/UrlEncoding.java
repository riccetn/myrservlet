package se.narstrom.myr.uri;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
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
			final String nameString = percentDecode(name);
			final String valueString = percentDecode(value);

			// 6. Append (nameString, valueString) to output.
			output.add(Map.entry(nameString, valueString));
		}

		// 4. Return output.
		return List.copyOf(output);
	}

	// https://url.spec.whatwg.org/#percent-decode
	public static String percentDecode(final String input) {
		return percentDecode(input, false);
	}

	public static String percentDecode(final String input, final boolean failOnInvalid) {
		// 1. Let output be an empty byte sequence.
		final int length = input.length();
		final ByteArrayOutputStream output = new ByteArrayOutputStream(length);

		// 2. For each byte byte in input:
		int index = 0;
		while (index < length) {
			final char ch = input.charAt(index);

			// 1. If byte is not 0x25 (%),
			if (ch != '%') {
				// then append byte to output.
				output.write(ch);
			}
			// 2. Otherwise, if byte is 0x25 (%) and the next two bytes after byte in input
			// are not in the ranges 0x30 (0) to 0x39 (9), 0x41 (A) to 0x46 (F), and 0x61
			// (a) to 0x66 (f), all inclusive
			else if ((length <= index + 2) || !isHexDigit(input.charAt(index + 1)) || !isHexDigit(input.charAt(index + 2))) {
				// append byte to output.
				if (failOnInvalid)
					throw new IllegalArgumentException("Invalid percent code");
				output.write(ch);
			}
			// 3. Otherwise:
			else {
				// 1. Let bytePoint be the two bytes after byte in input, decoded, and then
				// interpreted as hexadecimal number.
				final byte bytePoint = (byte) Integer.parseUnsignedInt(input, index + 1, index + 3, 16);

				// 2. Append a byte whose value is bytePoint to output.
				output.write(bytePoint);

				// 3. Skip the next two bytes in input.
				index += 2;
			}

			++index;
		}

		// 3. Return output.
		final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
		decoder.onMalformedInput(CodingErrorAction.REPORT);

		try {
			return decoder.decode(ByteBuffer.wrap(output.toByteArray())).toString();
		} catch (final CharacterCodingException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	private static boolean isHexDigit(final char ch) {
		return (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F') || (ch >= 'a' && ch <= 'f');
	}
}
