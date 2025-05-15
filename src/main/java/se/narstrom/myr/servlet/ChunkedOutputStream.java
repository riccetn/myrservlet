package se.narstrom.myr.servlet;

import java.io.IOException;
import java.io.OutputStream;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

public final class ChunkedOutputStream extends ServletOutputStream {
	private final OutputStream out;
	private boolean closed;

	public ChunkedOutputStream(final OutputStream out) {
		this.out = out;
	}

	@Override
	public boolean isReady() {
		throw new IllegalStateException("Async I/O is not supported");
	}

	@Override
	public void setWriteListener(final WriteListener writeListener) {
		throw new IllegalStateException("Async I/O is not supported");
	}

	@Override
	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
		out.write("0\r\n\r\n".getBytes());
		out.flush();
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		if(closed)
			throw new IOException("closed stream");
		final String hexlen = Integer.toHexString(len);
		out.write((hexlen + "\r\n").getBytes());
		out.write(b, off, len);
		out.write("\r\n".getBytes());
	}

	@Override
	public void write(final int b) throws IOException {
		write(new byte[] { (byte) b }, 0, 1);
	}
}
