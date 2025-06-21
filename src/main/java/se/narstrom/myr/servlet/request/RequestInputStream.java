package se.narstrom.myr.servlet.request;

import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

public class RequestInputStream extends ServletInputStream {
	private final InputStream in;

	private boolean finished = false;

	public RequestInputStream(final InputStream in) {
		this.in = in;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public boolean isReady() {
		throw new IllegalStateException("No async");
	}

	@Override
	public void setReadListener(ReadListener readListener) {
		throw new IllegalStateException("No async");
	}

	@Override
	public int read() throws IOException {
		final int ch = in.read();
		if (ch == -1)
			finished = true;
		return ch;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		final int ret = in.read(b, off, len);
		if (ret == -1)
			finished = true;
		return ret;
	}
}
