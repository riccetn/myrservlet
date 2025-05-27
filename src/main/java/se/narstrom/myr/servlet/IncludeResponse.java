package se.narstrom.myr.servlet;

import java.nio.charset.Charset;
import java.util.Locale;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

public final class IncludeResponse extends HttpServletResponseWrapper {
	public IncludeResponse(final HttpServletResponse response) {
		super(response);
	}

	@Override
	public void addCookie(Cookie cookie) {
		// NO-op
	}

	@Override
	public void addDateHeader(String name, long date) {
		// No-op
	}

	@Override
	public void addHeader(String name, String value) {
		// No-op
	}

	@Override
	public void addIntHeader(String name, int value) {
		// no-op
	}

	@Override
	public void setCharacterEncoding(Charset encoding) {
		// No-op
	}

	@Override
	public void setCharacterEncoding(String encoding) {
		// No-op
	}

	@Override
	public void setContentLength(int len) {
		// No-op
	}

	@Override
	public void setContentLengthLong(long len) {
		// No-op
	}

	@Override
	public void setContentType(final String type) {
		// No-op
	}

	@Override
	public void setDateHeader(String name, long date) {
		// No-op
	}

	@Override
	public void setHeader(String name, String value) {
		// No-op
	}

	@Override
	public void setIntHeader(String name, int value) {
		// No-op
	}

	@Override
	public void setLocale(Locale loc) {
		// No-op
	}

	@Override
	public void setStatus(int sc) {
		// No-op
	}
}
