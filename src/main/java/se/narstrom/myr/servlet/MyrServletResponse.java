package se.narstrom.myr.servlet;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import se.narstrom.myr.http.semantics.Token;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MyrServletResponse implements HttpServletResponse {
	private final Map<Token, List<String>> headerFields = new HashMap<>();

	private final OutputStream outputStream;

	int status = SC_OK;

	private PrintWriter writer;

	private boolean commited = false;

	public MyrServletResponse(final OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	public void commit() throws IOException {
		if (commited)
			return;
		commited = true;
		outputStream.write(("HTTP/1.1 " + status + "\r\n").getBytes());
		for (final Map.Entry<Token, List<String>> entry : headerFields.entrySet()) {
			final Token name = entry.getKey();
			final List<String> values = entry.getValue();
			for (final String value : values)
				outputStream.write((name.value() + ": " + value + "\r\n").getBytes());
		}
		outputStream.write("\r\n".getBytes());
	}

	@Override
	public void addCookie(final Cookie cookie) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsHeader(final String name) {
		return headerFields.containsKey(new Token(name));
	}

	@Override
	public String encodeURL(final String url) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String encodeRedirectURL(final String url) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCharacterEncoding() {
		return "ISO-8859-1";
	}

	@Override
	public String getContentType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (writer == null) {
			commit();
			writer = new PrintWriter(outputStream, false, Charset.forName(getCharacterEncoding(), StandardCharsets.ISO_8859_1));
		}
		return writer;
	}

	@Override
	public void setCharacterEncoding(final String charset) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setContentLength(final int len) {
		setHeader("content-length", Integer.toString(len));
	}

	@Override
	public void setContentLengthLong(final long len) {
		setHeader("content-length", Long.toString(len));
	}

	@Override
	public void setContentType(final String type) {
		setHeader("content-type", type);
	}

	@Override
	public void setBufferSize(final int size) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getBufferSize() {
		return 0;
	}

	@Override
	public void flushBuffer() throws IOException {
		commit();
		if (writer != null)
			writer.flush();
	}

	@Override
	public void resetBuffer() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCommitted() {
		return commited;
	}

	@Override
	public void reset() {
		if (commited)
			throw new IllegalStateException("Response has already been committed");
		status = SC_OK;
		headerFields.clear();
	}

	@Override
	public void setLocale(Locale loc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Locale getLocale() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		reset();
		setStatus(sc);
		setHeader("Content-Type", "text/plain");
		getWriter().write(msg);
		flushBuffer();
	}

	@Override
	public void sendError(int sc) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDateHeader(String name, long date) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addDateHeader(String name, long date) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setHeader(final String name, final String value) {
		headerFields.put(new Token(name), new ArrayList<>(List.of(value)));
	}

	@Override
	public void addHeader(String name, String value) {
		headerFields.computeIfAbsent(new Token(name), _ -> new ArrayList<>()).add(value);
	}

	@Override
	public void setIntHeader(final String name, final int value) {
		setHeader(name, Integer.toString(value));
	}

	@Override
	public void addIntHeader(final String name, final int value) {
		addHeader(name, Integer.toString(value));
	}

	@Override
	public void setStatus(final int status) {
		if (status < 100 || status >= 600)
			throw new IllegalArgumentException("Invalid status code: " + status);
		this.status = status;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public String getHeader(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<String> getHeaders(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<String> getHeaderNames() {
		throw new UnsupportedOperationException();
	}
}
