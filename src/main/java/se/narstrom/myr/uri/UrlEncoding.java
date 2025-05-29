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

		return result.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, ent -> List.copyOf(ent.getValue())));
	}

	// https://url.spec.whatwg.org/#percent-decode
	public static String percentDecode(final String input) {
		final int length = input.length();
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream(length);

		int index = 0;
		while (index < length) {
			final char ch = input.charAt(index);
			switch (ch) {
				case '+' -> {
					bytes.write(0x20);
					index += 1;
				}
				case '%' -> {
					if (index + 2 >= length)
						throw new IllegalArgumentException();
					final int b = Integer.parseUnsignedInt(input, index + 1, index + 3, 16);
					bytes.write(b);
					index += 3;
				}
				default -> {
					if (ch > 0x7F)
						throw new IllegalArgumentException();
					bytes.write((byte) ch);
					index += 1;
				}
			}
		}

		final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
		decoder.onMalformedInput(CodingErrorAction.REPORT);

		try {
			return decoder.decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
		} catch (final CharacterCodingException ex) {
			throw new IllegalArgumentException(ex);
		}
	}
}
