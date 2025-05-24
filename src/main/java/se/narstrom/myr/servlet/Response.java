package se.narstrom.myr.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import se.narstrom.myr.http.semantics.Token;
import se.narstrom.myr.mime.MediaType;
import se.narstrom.myr.util.Result;

public final class Response implements HttpServletResponse {
	private final Map<Token, List<String>> headerFields = new HashMap<>();

	private final OutputStream httpOutputStream;

	private final CommitingBufferedOutputStream commitingOutputStream;

	private Context context;

	private boolean outputStreamReturned = false;

	private OutputStream clientStream = null;

	private PrintWriter writer = null;

	private boolean commited = false;

	private int status = SC_OK;

	private Result<Charset, UnsupportedEncodingException> encoding = null;

	public Response(final OutputStream outputStream) {
		this.httpOutputStream = outputStream;
		this.commitingOutputStream = new CommitingBufferedOutputStream(this);
	}

	public OutputStream commit() throws IOException {
		if (commited)
			throw new IllegalStateException("Already commited");

		commited = true;
		httpOutputStream.write(("HTTP/1.1 " + status + "\r\n").getBytes());
		for (final Map.Entry<Token, List<String>> entry : headerFields.entrySet()) {
			final Token name = entry.getKey();
			final List<String> values = entry.getValue();
			for (final String value : values)
				httpOutputStream.write((name.value() + ": " + value + "\r\n").getBytes());
		}
		httpOutputStream.write("\r\n".getBytes());

		final String contentLength = getHeader("content-length");
		if (contentLength != null) {
			long length = -1L;
			try {
				length = Long.parseLong(contentLength);
			} catch (final NumberFormatException ex) {
			}
			if (length >= 0) {
				setHeader("transfer-encoding", null);
				clientStream = new LengthOutputStream(httpOutputStream, length);
			}
		}

		if (clientStream == null) {
			setHeader("transfer-encoding", "chunked");
			setHeader("content-length", null);
			clientStream = new ChunkedOutputStream(httpOutputStream);
		}

		return clientStream;
	}

	void setContext(final Context context) {
		this.context = context;
	}

	public void finish() throws IOException {
		if (writer != null)
			writer.flush();
		commitingOutputStream.close();
	}

	@Override
	public void addCookie(final Cookie cookie) {
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
			throw new IllegalStateException("Stream of writer, not both");
		outputStreamReturned = true;
		return commitingOutputStream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (writer == null) {
			if (outputStreamReturned)
				throw new IllegalStateException("Stream of writer, not both");

			if (encoding == null)
				setCharacterEncoding(StandardCharsets.ISO_8859_1);

			if (getContentType() == null)
				setContentType("text/plain");

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
		if(commited)
			throw new IllegalStateException();
		commitingOutputStream.setBufferSize(size);
	}

	@Override
	public int getBufferSize() {
		return 0;
	}

	@Override
	public void flushBuffer() throws IOException {
		if (writer != null)
			writer.flush();
		commitingOutputStream.flush();
	}

	@Override
	public void resetBuffer() {
		if (commited)
			throw new IllegalStateException("Response has already been committed");
		commitingOutputStream.resetBuffer();
	}

	@Override
	public boolean isCommitted() {
		return commited;
	}

	@Override
	public void reset() {
		if (commited)
			throw new IllegalStateException("Response has already been committed");
		commitingOutputStream.resetBuffer();
		outputStreamReturned = false;
		writer = null;
		clientStream = null;
		status = SC_OK;
		headerFields.clear();
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
			return null;
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
	public void setHeader(final String name, final String value) {
		final Token token = new Token(name.toLowerCase());
		if (value != null)
			headerFields.put(token, new ArrayList<>(List.of(value)));
		else
			headerFields.remove(token);
	}

	@Override
	public void addHeader(String name, String value) {
		headerFields.computeIfAbsent(new Token(name.toLowerCase()), _ -> new ArrayList<>()).add(value);
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
	public String getHeader(final String name) {
		final List<String> values = headerFields.get(new Token(name.toLowerCase()));
		if (values == null)
			return null;
		return values.getFirst();
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
