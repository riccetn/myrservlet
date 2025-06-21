package se.narstrom.myr.http.v1;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class LengthOutputStream extends FilterOutputStream {
	private long remaining;

	public LengthOutputStream(final OutputStream out, final long length) {
		super(out);
		this.remaining = length;
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
