package se.narstrom.myr.http.v2.hpack;

public final class BitStream {
	private final byte[] data;
	private int possition = 0;

	public BitStream(final byte[] data) {
		this.data = data;
	}

	public boolean getBit(final int offset) {
		final int byteOffset = offset / 8;
		final int shift = offset % 8;
		return ((data[byteOffset] >>> (7 - shift)) & 0x01) != 0;
	}

	public byte getByte(final int offset) {
		final int byteOffset = offset / 8;
		final int shift = offset % 8;
		return (byte) ((data[byteOffset] << shift) | (data[byteOffset + 1] >>> (8 - shift)));
	}

	public short getShort(final int offset) {
		final int byteOffset = offset / 8;
		final int shift = offset % 8;
		return (short) ((data[byteOffset] << (8 + shift)) | (data[byteOffset + 1] << shift) | (data[byteOffset + 2] >>> (8 - shift)));
	}

	public int getInt(final int offset) {
		final int byteOffset = offset / 8;
		final int shift = offset % 8;
		return ((data[byteOffset] << (24 + shift)) | (data[byteOffset + 1] << (16 + shift)) | (data[byteOffset + 2] << (8 + shift)) | (data[byteOffset + 3] << shift)
				| (data[byteOffset + 4] >>> (8 - shift)));
	}
}
