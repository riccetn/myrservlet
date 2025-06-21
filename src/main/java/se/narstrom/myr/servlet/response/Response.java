package se.narstrom.myr.servlet.response;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import se.narstrom.myr.http.HttpResponse;
import se.narstrom.myr.mime.MediaType;
import se.narstrom.myr.servlet.Context;
import se.narstrom.myr.util.Result;

public class Response implements HttpServletResponse {
	private final HttpResponse response;

	private final OuputBuffer buffer;

	private final Context context;

	private boolean outputStreamReturned = false;

	private PrintWriter writer = null;

	private Result<Charset, UnsupportedEncodingException> encoding = null;

	public Response(final HttpResponse response, final Context context) {
		this.response = response;
		this.buffer = new OuputBuffer(response);
		this.context = context;
	}

	public void close() throws IOException {
		if (writer != null)
			writer.flush();
		buffer.close();
	}

	@Override
	public void addCookie(Cookie cookie) {
		if (cookie.getMaxAge() == 0) {
			cookie = (Cookie) cookie.clone();
			cookie.setMaxAge(-1);
			cookie.setAttribute("Expires", "Thu, 1 Jan 1970 00:00:00 GMT");
		}

		final StringBuilder cookieString = new StringBuilder();
		cookieString.append(cookie.getName()).append("=").append(cookie.getValue());

		for (Map.Entry<String, String> attribute : cookie.getAttributes().entrySet()) {
			if (attribute.getValue().isEmpty()) {
				cookieString.append(";").append(attribute.getKey());
			} else
				cookieString.append(";").append(attribute.getKey()).append("=").append(attribute.getValue());
		}

		addHeader("Set-Cookie", cookieString.toString());
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
		return switch (this.encoding) {
			case Result.Ok(Charset set) -> set.name();
			case Result.Error(UnsupportedEncodingException ex) -> ex.getMessage();
			case null -> null;
		};
	}

	@Override
	public String getContentType() {
		return getHeader("content-type");
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (writer != null)
			throw new IllegalStateException("Stream or writer, not both");
		outputStreamReturned = true;
		return buffer;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (writer == null) {
			if (outputStreamReturned)
				throw new IllegalStateException("Stream of writer, not both");

			if (encoding == null)
				setCharacterEncoding(StandardCharsets.ISO_8859_1);

			// The PrintWriter constructors that take OutputStream creates an extra buffer,
			// so we manually create the OutputStreamWriter
			// to have better control of buffering
			writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), encoding.value()), false);
		}
		return writer;
	}

	@Override
	public void setCharacterEncoding(final String charset) {
		if (writer != null)
			return;

		setCharacterEncodingNoContentTypeUpdate(charset);
		updateContentTypeWithCharset(charset);
	}

	@Override
	public void setCharacterEncoding(final Charset encoding) {
		if (writer != null)
			return;

		if (encoding == null) {
			this.encoding = null;
			updateContentTypeWithCharset(null);
		} else {
			this.encoding = new Result.Ok<>(encoding);
			updateContentTypeWithCharset(encoding.name());
		}
	}

	private void updateContentTypeWithCharset(final String charset) {
		final String contentType = getHeader("content-type");
		if (contentType == null)
			return;

		final MediaType mediaType = MediaType.parse(contentType);
		final Map<String, String> parameters = new HashMap<>(mediaType.parameters());
		if (charset == null) {
			parameters.remove("charset");
		} else {
			parameters.put("charset", charset);
		}

		final MediaType newType = new MediaType(mediaType.type(), mediaType.subtype(), parameters);

		setHeader("content-type", newType.render());
	}

	private void setCharacterEncodingNoContentTypeUpdate(final String charset) {
		if (charset == null) {
			this.encoding = null;
		} else {
			try {
				this.encoding = new Result.Ok<>(Charset.forName(charset));
			} catch (final IllegalCharsetNameException | UnsupportedCharsetException _) {
				this.encoding = new Result.Error<>(new UnsupportedEncodingException(charset));
			}
		}
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
	public void setContentType(String contentType) {
		MediaType mediaType = MediaType.parse(contentType);
		String charset = mediaType.parameters().get("charset");

		if (charset != null && writer == null) {
			setCharacterEncodingNoContentTypeUpdate(charset);
		} else {
			charset = getCharacterEncoding();
			if (charset != null) {
				final Map<String, String> parameters = new HashMap<>(mediaType.parameters());
				parameters.put("charset", charset);
				mediaType = new MediaType(mediaType.type(), mediaType.subtype(), parameters);
				contentType = mediaType.render();
			}
		}

		setHeader("content-type", contentType);
	}

	@Override
	public void setBufferSize(final int size) {
		if (isCommitted())
			throw new IllegalStateException();
		buffer.setBufferSize(size);
	}

	@Override
	public int getBufferSize() {
		return 0;
	}

	@Override
	public void flushBuffer() throws IOException {
		if (writer != null)
			writer.flush();
		buffer.flush();
	}

	@Override
	public void resetBuffer() {
		if (isCommitted())
			throw new IllegalStateException("Response has already been committed");
		buffer.resetBuffer();
	}

	@Override
	public void reset() {
		if (isCommitted())
			throw new IllegalStateException("Response has already been committed");
		buffer.resetBuffer();
		outputStreamReturned = false;
		writer = null;
	}

	@Override
	public void setLocale(final Locale locale) {
		setHeader("content-language", locale.toLanguageTag());
		if (encoding == null) {
			final Charset charset = context.getLocaleEncoding(locale);
			if (charset != null)
				setCharacterEncoding(charset);
		}
	}

	@Override
	public Locale getLocale() {
		final String contentLanguage = getHeader("content-language");
		if (contentLanguage == null)
			return Locale.getDefault();
		return Locale.forLanguageTag(contentLanguage);
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		reset();
		setStatus(sc);
		setContentType("text/plain");
		setContentLength(msg.length());
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
	public void setIntHeader(final String name, final int value) {
		setHeader(name, Integer.toString(value));
	}

	@Override
	public void addIntHeader(final String name, final int value) {
		addHeader(name, Integer.toString(value));
	}

	@Override
	public boolean isCommitted() {
		return response.isCommitted();
	}

	@Override
	public boolean containsHeader(final String name) {
		return response.containsHeader(name);
	}

	@Override
	public void setHeader(final String name, final String value) {
		response.setHeader(name, value);
	}

	@Override
	public void addHeader(final String name, final String value) {
		response.addHeader(name, value);
	}

	@Override
	public void setStatus(int status) {
		response.setStatus(status);
	}

	@Override
	public int getStatus() {
		return response.getStatus();
	}

	@Override
	public String getHeader(final String name) {
		return response.getHeader(name);
	}

	@Override
	public Collection<String> getHeaders(final String name) {
		return response.getHeaders(name);
	}

	@Override
	public Collection<String> getHeaderNames() {
		return response.getHeaderNames();
	}
}
