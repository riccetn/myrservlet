package se.narstrom.myr.servlet;

import java.io.BufferedReader;
import java.io.IOException;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public final class DispatcherRequest extends HttpServletRequestWrapper {
	public DispatcherRequest(final HttpServletRequest request) {
		super(request);
	}

	private boolean streamReturned;

	private boolean readerReturned;

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (readerReturned)
			throw new IllegalStateException("Stream or Reader not both");
		streamReturned = true;
		return super.getInputStream();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		if (streamReturned)
			throw new IllegalStateException("Stream or Reader not both");
		readerReturned = true;
		return super.getReader();
	}
}
