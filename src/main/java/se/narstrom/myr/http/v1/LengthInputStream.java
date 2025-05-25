package se.narstrom.myr.http.v1;

import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

public final class LengthInputStream extends ServletInputStream {
	private final InputStream in;
	private long remaining;
	private long mark = -1;

	public LengthInputStream(final InputStream in, final long length) {
		this.in = in;
		this.remaining = length;
	}

	@Override
	public int read() throws IOException {
		if (remaining == 0)
			return -1;
		remaining -= 1;
		int result = in.read();
		if (result == -1) {
			throw new IOException("Early EOF");
		}
		return result;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (remaining == 0)
			return -1;
		if (len > remaining)
			len = (int) remaining;
		int read = in.read(b, off, len);
		if (read == -1) {
			throw new IOException("Early EOF");
		}
		remaining -= read;
		return read;
	}

	@Override
	public long skip(long n) throws IOException {
		if (remaining == 0)
			return 0L;
		if (n > remaining)
			n = remaining;
		long result = in.skip(n);
		remaining -= result;
		return result;
	}

	@Override
	public int available() throws IOException {
		int avaiable = in.available();
		if (avaiable > remaining)
			avaiable = (int) remaining;
		return avaiable;
	}

	@Override
	public void close() throws IOException {
		while (remaining > 0)
			skip(remaining);
		// Leave sockets InputStream open for keep-alive
	}

	@Override
	public void mark(final int readlimit) {
		mark = remaining;
		super.mark(readlimit);
	}

	@Override
	public void reset() throws IOException {
		if (mark == -1)
			throw new IOException("reset() called before mark()");
		super.reset();
		remaining = mark;
	}

	@Override
	public boolean isFinished() {
		return remaining == 0L;
	}

	@Override
	public boolean isReady() {
		throw new IllegalStateException("Async I/O is not supported");
	}

	@Override
	public void setReadListener(final ReadListener readListener) {
		throw new IllegalStateException("Async I/O is not supported");
	}
}
