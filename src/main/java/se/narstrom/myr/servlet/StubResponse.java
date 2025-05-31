package se.narstrom.myr.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public abstract class StubResponse implements HttpServletResponse {

	@Override
	public void addCookie(final Cookie cookie) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void addDateHeader(final String name, final long date) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void addHeader(final String name, final String value) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void addIntHeader(final String name, final int value) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public boolean containsHeader(final String name) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String encodeRedirectURL(final String url) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String encodeURL(final String url) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void flushBuffer() throws IOException {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public int getBufferSize() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getCharacterEncoding() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getContentType() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getHeader(final String name) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public Collection<String> getHeaderNames() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public Collection<String> getHeaders(final String name) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public Locale getLocale() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public int getStatus() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public boolean isCommitted() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void reset() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void resetBuffer() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void sendError(final int sc) throws IOException {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void sendError(final int sc, final String msg) throws IOException {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void sendRedirect(final String location, final int sc, final boolean clearBuffer) throws IOException {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void setBufferSize(final int size) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void setCharacterEncoding(final String encoding) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void setContentLength(final int len) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void setContentLengthLong(final long len) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void setContentType(final String type) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void setDateHeader(final String name, final long date) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void setHeader(final String name, final String value) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void setIntHeader(final String name, final int value) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void setLocale(final Locale loc) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void setStatus(final int sc) {
		throw new UnsupportedOperationException("Stub");
	}
}
