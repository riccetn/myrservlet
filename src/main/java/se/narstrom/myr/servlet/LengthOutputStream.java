package se.narstrom.myr.servlet;

import java.io.IOException;
import java.io.OutputStream;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

public final class LengthOutputStream extends ServletOutputStream {
	private final OutputStream out;
	private long remaining;

	public LengthOutputStream(final OutputStream out, final long length) {
		this.out = out;
		this.remaining = length;
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
		if (remaining != 0)
			throw new IOException("close() before Content-Length amount of data");
		// Keep socket OutputStream open for keep-alive
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		if (len > remaining)
			throw new IOException("Overrun Content-Length");
		out.write(b, off, len);
		remaining -= len;
	}

	@Override
	public void write(final int b) throws IOException {
		if (remaining == 0)
			throw new IOException("Overrun Content-Length");
		out.write(b);
		remaining -= 1;
	}

}
