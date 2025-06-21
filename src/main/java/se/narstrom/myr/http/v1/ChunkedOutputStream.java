package se.narstrom.myr.http.v1;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class ChunkedOutputStream extends FilterOutputStream {
	private boolean closed;

	public ChunkedOutputStream(final OutputStream out) {
		super(out);
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
		if (closed)
			throw new IOException("closed stream");
		if (len == 0)
			return;
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
