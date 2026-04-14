package se.narstrom.myr.web.url;

import java.nio.charset.StandardCharsets;

import se.narstrom.myr.web.infra.ByteSequence;
import se.narstrom.myr.web.infra.ByteSequenceBuilder;

// https://url.spec.whatwg.org/#percent-encoded-bytes
public final class PercentEncoded {
	public static final String percentEncode(final byte b) {
		if (b < 0x10)
			return "%0" + Integer.toHexString(Byte.toUnsignedInt(b)).toUpperCase();
		else
			return "%" + Integer.toHexString(Byte.toUnsignedInt(b)).toUpperCase();
	}

	public static final ByteSequence percentDecode(final ByteSequence input) {
		return percentDecodeOrThrow(input, null);
	}

	public static final <E extends Exception> ByteSequence percentDecodeOrThrow(final ByteSequence input, final Class<E> exceptionClazz) throws E {
		// 1. Let output be an empty byte sequence.
		final ByteSequenceBuilder output = new ByteSequenceBuilder();

		// 2. For each byte byte in input:
		int index = 0;
		while (index < input.size()) {
			final int b = input.getUnsignedValue(index);

			// 1. If byte is not 0x25 (%),
			if (b != '%') {
				// then append byte to output.
				output.add(b);
			}
			// 2. Otherwise, if byte is 0x25 (%) and the next two bytes after byte in input
			// are not in the ranges 0x30 (0) to 0x39 (9), 0x41 (A) to 0x46 (F), and 0x61
			// (a) to 0x66 (f), all inclusive
			else if ((input.size() <= index + 2) || !isHexDigit(input.getUnsignedValue(index + 1)) || !isHexDigit(input.getUnsignedValue(index + 2))) {
				if (exceptionClazz != null) {
					try {
						throw exceptionClazz.getConstructor().newInstance();
					} catch (final ReflectiveOperationException ex) {
						throw new IllegalArgumentException(ex);
					}
				}

				// append byte to output.
				output.add(b);
			}
			// 3. Otherwise:
			else {
				// 1. Let bytePoint be the two bytes after byte in input, decoded, and then
				// interpreted as hexadecimal number.
				final int bytePoint = Integer.parseUnsignedInt(input.subList(index + 1, index + 3).toString(), 16);

				// 2. Append a byte whose value is bytePoint to output.
				output.add(bytePoint);

				// 3. Skip the next two bytes in input.
				index += 2;
			}

			index += 1;
		}

		// 3. Return output.
		return output.toByteSequence();
	}

	public static final ByteSequence percentDecode(final String input) {
		// 1. Let bytes be the UTF-8 encoding of input.
		final ByteSequence bytes = new ByteSequence(input, StandardCharsets.UTF_8);

		// 2. Return the percent-decoding of bytes.
		return percentDecode(bytes);
	}

	public static final <E extends Exception> ByteSequence percentDecodeOrThrow(final String input, final Class<E> exceptionClazz) throws E {
		// 1. Let bytes be the UTF-8 encoding of input.
		final ByteSequence bytes = new ByteSequence(input, StandardCharsets.UTF_8);

		// 2. Return the percent-decoding of bytes.
		return percentDecodeOrThrow(bytes, exceptionClazz);
	}

	public static final String percentDecodeToString(final ByteSequence input) {
		return percentDecode(input).toString(StandardCharsets.UTF_8);
	}
	
	public static final String percentDecodeToString(final String input) {
		return percentDecode(input).toString(StandardCharsets.UTF_8);
	}

	private static boolean isHexDigit(final int cp) {
		return (cp >= '0' && cp <= '9') || (cp >= 'A' && cp <= 'F') || (cp >= 'a' && cp <= 'f');
	}
}
