package se.narstrom.myr.servlet.response;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;

public final class OuputBuffer extends ServletOutputStream {
	private final HttpServletResponse response;

	private ByteBuffer buffer;

	private OutputStream out;

	public OuputBuffer(final HttpServletResponse response) {
		this.buffer = ByteBuffer.allocate(1500);
		this.response = response;
	}

	void resetBuffer() {
		buffer.clear();
	}

	void setBufferSize(int size) {
		if(response.isCommitted() || buffer.position() > 0)
			throw new IllegalStateException();
		buffer = ByteBuffer.allocate(size);
	}

	@Override
	public void flush() throws IOException {
		if(out == null) {
			out = response.getOutputStream();
		}
		buffer.flip();
		if(buffer.remaining() > 0)
			out.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
		buffer.clear();
	}

	@Override
	public void close() throws IOException {
		flush();
		out.close();
	}

	@Override
	public boolean isReady() {
		throw new IllegalStateException("No async");
	}

	@Override
	public void setWriteListener(WriteListener writeListener) {
		throw new IllegalStateException("No async");
	}

	@Override
	public void write(final int b) throws IOException {
		if (buffer.remaining() < 1)
			flush();
		buffer.put((byte) b);
	}

	@Override
	public void write(final byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		if (buffer.remaining() < len)
			flush();

		if (buffer.remaining() >= len)
			buffer.put(b, off, len);
		else
			out.write(b, off, len);
	}

	@Override
	public void write(final ByteBuffer sourceBuffer) throws IOException {
		if (buffer.remaining() < sourceBuffer.remaining())
			flush();

		if (buffer.remaining() >= sourceBuffer.remaining())
			buffer.put(sourceBuffer);
		else {
			if (sourceBuffer.hasArray()) {
				out.write(sourceBuffer.array(), sourceBuffer.arrayOffset() + sourceBuffer.position(), sourceBuffer.remaining());
			} else {
				final byte[] sourceData = new byte[sourceBuffer.remaining()];
				sourceBuffer.get(sourceData);
				out.write(sourceData);
			}
		}
	}
}
