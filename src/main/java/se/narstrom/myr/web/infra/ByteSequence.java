package se.narstrom.myr.web.infra;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Objects;

// https://infra.spec.whatwg.org/#byte-sequences
public final class ByteSequence extends AbstractList<Byte> {
	private final byte[] bytes;
	private final int from;
	private final int length;

	private ByteSequence(final byte[] bytes, final int from, final int length) {
		this.bytes = bytes;
		this.from = from;
		this.length = length;
	}

	public ByteSequence(final byte[] bytes) {
		this(bytes.clone(), 0, bytes.length);
	}

	public ByteSequence(final CharSequence chars, final Charset charset) {
		final ByteBuffer buffer = charset.encode(CharBuffer.wrap(chars));
		this(buffer.array(), buffer.arrayOffset(), buffer.limit());
	}

	public ByteSequence(final CharSequence chars) {
		this(chars, StandardCharsets.UTF_8);
	}

	@Override
	public int size() {
		return length;
	}

	@Override
	public Byte get(final int index) {
		Objects.checkIndex(index, length);
		return bytes[from + index];
	}

	@Override
	public ByteSequence subList(final int from, final int to) {
		Objects.checkFromToIndex(from, to, length);
		return new ByteSequence(bytes, from, to - from);
	}

	public byte getByte(final int index) {
		Objects.checkIndex(index, length);
		return bytes[index];
	}

	public int getUnsignedValue(final int index) {
		Objects.checkIndex(index, length);
		return bytes[index] & 0xFF;
	}

	@Override
	public String toString() {
		return new String(bytes, from, length, StandardCharsets.UTF_8);
	}

	public String toString(final Charset charset) {
		return new String(bytes, from, length, charset);
	}

	public byte[] toByteArray() {
		return Arrays.copyOfRange(bytes, from, from + length);
	}
}
