package se.narstrom.myr.servlet.response;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import se.narstrom.myr.http.HttpResponse;
import se.narstrom.myr.mime.MediaType;
import se.narstrom.myr.servlet.context.Context;
import se.narstrom.myr.servlet.dispatcher.Dispatcher;

// 5. The Response
// ===============
// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#the-response
public class Response implements HttpServletResponse {
	private final HttpResponse response;

	private Dispatcher dispatcher;

	public Response(final HttpResponse response) {
		this.response = response;
		this.buffer = new OuputBuffer(this);
	}

	public void setDispatcher(final Dispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	public void close() throws IOException {
		if (writer != null)
			writer.flush();
		buffer.close();
	}

	public void commit() throws IOException {
		final ServletRequest request = dispatcher.getRequest();
		if (!(request instanceof HttpServletRequest httpRequest)) {
			return;
		}
		final HttpSession session = httpRequest.getSession(false);
		if (session != null && session.isNew()) {
			addCookie(new Cookie("JSESSIONID", session.getId()));
		}
		response.commit();
	}

	OutputStream getRealOutputStream() throws IOException {
		return response.getOutputStream();
	}

	// 5.1 Buffering
	// =============
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#buffering
	private final OuputBuffer buffer;
	private PrintWriter writer = null;
	private boolean outputStreamReturned = false;
	private boolean writerReturned = false;

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (writerReturned)
			throw new IllegalStateException("Stream or writer, not both");
		outputStreamReturned = true;
		return buffer;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (outputStreamReturned)
			throw new IllegalStateException("Stream of writer, not both");

		if (writer == null) {
			if (this.charset == null) {
				try {
					this.charset = Charset.forName(characterEncoding);
				} catch (final UnsupportedCharsetException ex) {
					throw new UnsupportedEncodingException(ex.getMessage());
				}
			}

			// The PrintWriter constructors that take OutputStream creates an extra buffer,
			// so we manually create the OutputStreamWriter to have better control over
			// buffering
			writer = new PrintWriter(new OutputStreamWriter(buffer, this.charset), false);
		}

		writerReturned = true;
		return writer;
	}

	@Override
	public int getBufferSize() {
		return buffer.getBufferSize();
	}

	@Override
	public void setBufferSize(final int size) {
		if (isCommitted())
			throw new IllegalStateException();
		buffer.setBufferSize(size);
	}

	@Override
	public boolean isCommitted() {
		return response.isCommitted();
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
	public void resetBuffer() {
		if (isCommitted())
			throw new IllegalStateException("Response has already been committed");
		buffer.resetBuffer();
	}

	@Override
	public void flushBuffer() throws IOException {
		if (writer != null)
			writer.flush();
		buffer.flush();
	}


	// 5.2. Headers
	// ============
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#headers-2
	@Override
	public void setHeader(final String name, final String value) {
		response.setHeader(name, value);
	}

	@Override
	public void addHeader(final String name, final String value) {
		response.addHeader(name, value);
	}

	@Override
	public void setDateHeader(final String name, final long millis) {
		final ZonedDateTime datetime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
		setHeader(name, datetime.format(DateTimeFormatter.RFC_1123_DATE_TIME));
	}

	@Override
	public void addDateHeader(final String name, final long millis) {
		final ZonedDateTime datetime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
		addHeader(name, datetime.format(DateTimeFormatter.RFC_1123_DATE_TIME));
	}

	@Override
	public void setIntHeader(final String name, final int value) {
		setHeader(name, Integer.toString(value));
	}

	@Override
	public void addIntHeader(final String name, final int value) {
		addHeader(name, Integer.toString(value));
	}


	// 5.3 HTTP Trailers
	// =================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#http-trailers
	private Supplier<Map<String, String>> trailerFieldsSupplier = null;

	@Override
	public void setTrailerFields(final Supplier<Map<String, String>> supplier) {
		this.trailerFieldsSupplier = supplier;
	}

	@Override
	public Supplier<Map<String, String>> getTrailerFields() {
		return trailerFieldsSupplier;
	}


	// 5.5. Convenience Methods
	// ========================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#convenience-methods
	@Override
	public void sendError(final int statusCode, final String message) throws IOException {
		final Context context = this.dispatcher.getContext();
		context.handleError(dispatcher.getOriginalRequest(), this, statusCode, message, null);
	}

	@Override
	public void sendError(final int statusCode) throws IOException {
		sendError(statusCode, "");
	}

	@Override
	public void sendRedirect(final String location, final int sc, final boolean clearBuffer) throws IOException {
		if (isCommitted())
			throw new IllegalStateException();

		if (clearBuffer)
			resetBuffer();

		final URI requestUri = URI.create(dispatcher.getOriginalRequest().getRequestURL().toString());
		final URI locationUri = requestUri.resolve(location);

		setStatus(SC_FOUND);
		setHeader("location", locationUri.toString());
		setContentType("text/plain");
		getWriter().write("Redirect to: " + location);
		flushBuffer();
	}


	// 5.6. Internationalization
	// =========================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#convenience-methods
	private boolean servletSpecifiedEncoding = false;
	private String characterEncoding = "ISO-8859-1";
	private Charset charset = StandardCharsets.ISO_8859_1;
	private Locale locale = Locale.getDefault();

	@Override
	public void setCharacterEncoding(final String charset) {
		if (writerReturned || isCommitted())
			return;

		if (charset == null) {
			this.charset = StandardCharsets.ISO_8859_1;
			this.characterEncoding = StandardCharsets.ISO_8859_1.name();
			this.servletSpecifiedEncoding = false;
		} else {
			this.characterEncoding = charset;
			this.charset = null;
			this.servletSpecifiedEncoding = true;
		}

		final String contentType = getContentType();
		if (contentType != null) {
			final MediaType oldMediaType = MediaType.parse(contentType);
			final Map<String, String> params = new HashMap<>(oldMediaType.parameters());
			if (charset != null)
				params.put("charset", charset);
			else
				params.remove("charset");
			final MediaType newMediaType = new MediaType(oldMediaType.type(), oldMediaType.subtype(), params);
			setHeader("content-type", newMediaType.render());
		}
	}

	@Override
	public void setCharacterEncoding(final Charset encoding) {
		if (writerReturned || isCommitted())
			return;

		setCharacterEncoding(encoding.name());
		this.charset = encoding;
	}

	@Override
	public String getCharacterEncoding() {
		return characterEncoding;
	}

	@Override
	public String getContentType() {
		return getHeader("content-type");
	}

	@Override
	public void setContentType(final String contentType) {
		MediaType newMediaType = MediaType.parse(contentType);

		String charset = newMediaType.parameters().get("charset");

		if (charset != null)
			setCharacterEncoding(charset);

		if (writerReturned || charset == null)
			charset = this.characterEncoding;

		final HashMap<String, String> params = new HashMap<>(newMediaType.parameters());
		params.put("charset", characterEncoding);
		newMediaType = new MediaType(newMediaType.type(), newMediaType.subtype(), params);

		setHeader("content-type", newMediaType.render());
	}

	@Override
	public void setLocale(final Locale locale) {
		if (isCommitted())
			return;

		this.locale = locale;
		setHeader("content-language", locale.toLanguageTag());
		if (!servletSpecifiedEncoding) {
			final Charset charset = dispatcher.getContext().getLocaleEncoding(locale);
			if (charset != null) {
				this.characterEncoding = charset.name();
				this.charset = charset;
			}
		}
	}

	@Override
	public Locale getLocale() {
		return locale;
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
	public void setContentLength(final int len) {
		setHeader("content-length", Integer.toString(len));
	}

	@Override
	public void setContentLengthLong(final long len) {
		setHeader("content-length", Long.toString(len));
	}

	@Override
	public boolean containsHeader(final String name) {
		return response.containsHeader(name);
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
