package se.narstrom.myr.web.infra;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Objects;

public final class ByteSequenceBuilder extends AbstractList<Byte> {
	private byte[] bytes;
	private int length;
	
	public ByteSequenceBuilder() {
		this.bytes = new byte[16];
		this.length = 0;
	}

	@Override
	public int size() {
		return length;
	}

	@Override
	public Byte get(final int index) {
		Objects.checkIndex(index, length);
		return bytes[index];
	}

	public byte getByte(final int index) {
		Objects.checkIndex(index, length);
		return bytes[index];
	}

	public int getUnsignedValue(final int index) {
		Objects.checkIndex(index, length);
		return Byte.toUnsignedInt(bytes[index]);
	}

	@Override
	public Byte set(final int index, final Byte newValue) {
		Objects.checkIndex(index, length);
		Objects.requireNonNull(newValue);
		final byte oldValue = bytes[index];
		bytes[index] = newValue;
		return oldValue;
	}

	public byte set(final int index, final byte newValue) {
		Objects.checkIndex(index, length);
		final byte oldValue = bytes[index];
		bytes[index] = newValue;
		return oldValue;
	}

	@Override
	public void add(final int index, final Byte value) {
		add(index, (byte) value);
	}

	public void add(int value) {
		add(length, (byte) value);
	}

	public void add(final int index, final byte value) {
		Objects.checkIndex(index, length + 1);

		final int oldLength = length;
		final int oldCapacity = bytes.length;
		final int newLength = oldLength + 1;

		if (newLength > oldCapacity) {
			final int newCapacity = Math.max(bytes.length * 2, 16);
			final byte[] newBytes = new byte[newCapacity];
			System.arraycopy(bytes, 0, newBytes, 0, index);
			System.arraycopy(bytes, index, newBytes, index + 1, oldLength - index);
			bytes = newBytes;
		} else {
			System.arraycopy(bytes, index, bytes, index + 1, oldLength - index);
		}

		bytes[index] = value;
		length = newLength;
	}

	@Override
	public String toString() {
		return new String(bytes, 0, length, StandardCharsets.UTF_8);
	}

	public String toString(final Charset charset) {
		return new String(bytes, 0, length, charset);
	}

	public byte[] toByteArray() {
		return Arrays.copyOfRange(bytes, 0, length);
	}

	public ByteSequence toByteSequence() {
		return new ByteSequence(toByteArray());
	}
}
