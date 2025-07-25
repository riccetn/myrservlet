package se.narstrom.myr.http.v1;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import se.narstrom.myr.MappingMap;
import se.narstrom.myr.http.semantics.Fields;

public class ChunkedInputStream extends FilterInputStream {
	private Http1InputStream in;
	private ByteBuffer chunk = null;
	private boolean finished = false;
	private Fields trailerFields = null;

	public ChunkedInputStream(final Http1InputStream in) {
		super(in);
		this.in = in;
	}

	@Override
	public int read() throws IOException {
		if (finished)
			return -1;
		if (chunk == null || !chunk.hasRemaining()) {
			if (!readChunk())
				return -1;
		}
		return chunk.get();
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		if (finished)
			return 0;
		if (chunk == null || !chunk.hasRemaining()) {
			if (!readChunk())
				return 0;
		}
		final int realLen = Math.min(len, chunk.remaining());
		chunk.get(b, off, realLen);
		return realLen;
	}

	private boolean readChunk() throws IOException {
		final int len = Integer.parseUnsignedInt(in.readLine(), 16);

		if (len == 0) {
			trailerFields = in.readFields();
			return false;
		}

		byte[] bytes = in.readNBytes(len);
		if (bytes.length != len)
			throw new EOFException("Unexpected end-of-stream");

		int cr = in.read();
		int lf = in.read();
		if (cr != '\r' || lf != '\n')
			throw new IOException("Invalid chunked encoding");

		this.chunk = ByteBuffer.wrap(bytes);

		return true;
	}

	public boolean isTrailerFieldsReady() {
		return trailerFields != null;
	}

	public Map<String, String> getTrailerFields() {
		if (trailerFields == null)
			throw new IllegalStateException("Trailer is not ready");
		return new MappingMap<>(trailerFields.getFieldMap(), name -> name.toString(), values -> values.getFirst().toString());
	}
}
