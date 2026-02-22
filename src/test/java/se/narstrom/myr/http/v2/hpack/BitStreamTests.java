package se.narstrom.myr.http.v2.hpack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BitStreamTests {
	private static final byte[] DATA2 = { (byte) 0xF0, 0x12 };
	private static final byte[] DATA3 = { (byte) 0xF0, 0x12, 0x34 };

	static Stream<Arguments> getBitsSource() {
		// @formatter:off
		return Stream.of(
				Arguments.of(true, DATA2, 0),
				Arguments.of(true, DATA2, 1),
				Arguments.of(true, DATA2, 2),
				Arguments.of(true, DATA2, 3),
				Arguments.of(false, DATA2, 4),
				Arguments.of(false, DATA2, 5),
				Arguments.of(false, DATA2, 6),
				Arguments.of(false, DATA2, 7),

				Arguments.of(false, DATA2, 8),
				Arguments.of(false, DATA2, 9),
				Arguments.of(false, DATA2, 10),
				Arguments.of(true, DATA2, 11),
				Arguments.of(false, DATA2, 12),
				Arguments.of(false, DATA2, 13),
				Arguments.of(true, DATA2, 14),
				Arguments.of(false, DATA2, 15));
		// @formatter:on
	}

	@ParameterizedTest
	@MethodSource("getBitsSource")
	public void getBitTests(final boolean expected, final byte[] bytes, final int index) {
		final BitStream stream = new BitStream(bytes);
		assertEquals(expected, stream.getBit(index));
	}

	@Test
	public void getBitTest_outOfBounds() {
		final BitStream stream = new BitStream(DATA2);
		assertThrows(IndexOutOfBoundsException.class, () -> stream.getBit(16));
	}

	static Stream<Arguments> getByteSource() {
		// @formatter:on
		return Stream.of(
				Arguments.of((byte) 0b1111_0000, DATA3, 0),
				Arguments.of((byte) 0b1110_0000, DATA3, 1),
				Arguments.of((byte) 0b1100_0000, DATA3, 2),
				Arguments.of((byte) 0b1000_0000, DATA3, 3),
				Arguments.of((byte) 0b0000_0001, DATA3, 4),
				Arguments.of((byte) 0b0000_0010, DATA3, 5),
				Arguments.of((byte) 0b0000_0100, DATA3, 6),
				Arguments.of((byte) 0b0000_1001, DATA3, 7),

				Arguments.of((byte) 0b0001_0010, DATA3, 8),
				Arguments.of((byte) 0b0010_0100, DATA3, 9),
				Arguments.of((byte) 0b0100_1000, DATA3, 10),
				Arguments.of((byte) 0b1001_0001, DATA3, 11),
				Arguments.of((byte) 0b0010_0011, DATA3, 12),
				Arguments.of((byte) 0b0100_0110, DATA3, 13),
				Arguments.of((byte) 0b1000_1101, DATA3, 14),
				Arguments.of((byte) 0b0001_1010, DATA3, 15),

				Arguments.of((byte) 0b0011_0100, DATA3, 16));
		// @formatter:off
	}

	void assertBytesEquals(byte expected, byte actual) {
		assertEquals(expected, actual, () -> String.format("%s != %s%n", Integer.toBinaryString(expected & 0xFF), Integer.toBinaryString(actual & 0xFF)));
	}

	@ParameterizedTest
	@MethodSource("getByteSource")
	public void getByteTests(final byte expected, final byte[] data, final int index) {
		final BitStream stream = new BitStream(data);
		assertBytesEquals(expected, stream.getByte(index));
	}

	@Test
	public void getByteTest_outOfBounds() {
		final BitStream stream = new BitStream(DATA3);
		assertThrows(IndexOutOfBoundsException.class, () -> stream.getByte(17));
	}
}
