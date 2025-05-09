package se.narstrom.myr.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.security.Principal;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import se.narstrom.myr.http.semantics.Method;
import se.narstrom.myr.http.semantics.Token;
import se.narstrom.myr.http.v1.AbsolutePath;
import se.narstrom.myr.mime.MediaType;
import se.narstrom.myr.uri.Query;
import se.narstrom.myr.uri.UrlEncoding;

public final class MyrServletRequest implements HttpServletRequest {
	private final Logger logger = Logger.getLogger(getClass().getName());

	private final Method method;

	private final AbsolutePath path;

	private final Query query;

	private final Map<Token, List<String>> fields;

	private final Socket socket;

	private final InputStream inputStream;

	private final Map<String, Object> attributes = new HashMap<>();

	private Map<String, List<String>> parameters = null;

	private ServletInputStream clientInputStream = null;

	private Charset encoding = null;

	public MyrServletRequest(final Method method, final AbsolutePath path, final Query query, final Map<Token, List<String>> fields, final Socket socket, final InputStream inputStream) {
		this.method = method;
		this.path = path;
		this.query = query;
		this.fields = fields;
		this.socket = socket;
		this.inputStream = inputStream;
	}

	@Override
	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(attributes.keySet());
	}

	@Override
	public String getCharacterEncoding() {
		if (encoding != null)
			return encoding.name();

		final String contentType = getContentType();
		if (contentType != null) {
			try {
				final String charset = MediaType.parse(contentType).parameters().get("charset");
				if(charset != null)
					encoding = Charset.forName(charset);
			} catch (final IllegalCharsetNameException | UnsupportedCharsetException | ParseException ex) {
				logger.log(Level.WARNING, "Failed to get charset from content-type header field", ex);
			}
		}

		if (encoding == null)
			encoding = StandardCharsets.UTF_8;

		return encoding.name();
	}

	@Override
	public void setCharacterEncoding(final String encoding) throws UnsupportedEncodingException {
		try {
			this.encoding = Charset.forName(encoding);
		} catch (final IllegalCharsetNameException | UnsupportedCharsetException ex) {
			throw new UnsupportedEncodingException(ex.toString());
		}
	}

	@Override
	public void setCharacterEncoding(final Charset encoding) {
		this.encoding = encoding;
	}

	@Override
	public int getContentLength() {
		return getIntHeader("content-length");
	}

	@Override
	public long getContentLengthLong() {
		final String contentLength = getHeader("content-length");
		if (contentLength == null)
			return -1L;
		try {
			return Long.parseLong(contentLength);
		} catch (final NumberFormatException ex) {
			return -1L;
		}
	}

	@Override
	public String getContentType() {
		return getHeader("content-type");
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (clientInputStream == null) {
			final long length = getContentLengthLong();
			if (length != -1L) {
				clientInputStream = new LengthInputStream(inputStream, length);
			}
		}
		return clientInputStream;
	}

	@Override
	public String getParameter(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Enumeration<String> getParameterNames() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String[] getParameterValues(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		maybeInitParameters();
		return parameters.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toArray(String[]::new)));
	}

	private void maybeInitParameters() {
		if (parameters != null)
			return;

		parameters = new HashMap<>();

		if (!query.value().isEmpty()) {
			final Map<String, List<String>> queryParameters = UrlEncoding.parse(query.value());
			for (final Map.Entry<String, List<String>> ent : queryParameters.entrySet()) {
				parameters.computeIfAbsent(ent.getKey(), _ -> new ArrayList<>()).addAll(ent.getValue());
			}
		}

		final String contentType = getContentType();
		MediaType mediaType = null;
		if (contentType != null) {
			try {
				mediaType = MediaType.parse(contentType);
			} catch (final ParseException ex) {
			}
		}

		if (mediaType != null && mediaType.type().equals("application") && mediaType.subtype().equals("x-www-form-urlencoded")) {
			getCharacterEncoding();

			byte[] contentBytes = null;
			try {
				contentBytes = getInputStream().readAllBytes();
			} catch (final IOException ex) {
				logger.log(Level.SEVERE, "Failed to read body for parameters", ex);
			}
			if (contentBytes != null) {
				final String content = new String(contentBytes, encoding);
				final Map<String, List<String>> contentParams = UrlEncoding.parse(content);
				for (final Map.Entry<String, List<String>> ent : contentParams.entrySet()) {
					parameters.computeIfAbsent(ent.getKey(), _ -> new ArrayList<>()).addAll(ent.getValue());
				}
			}
		}
	}

	@Override
	public String getProtocol() {
		return "HTTP/1.1";
	}

	@Override
	public String getScheme() {
		return "http";
	}

	@Override
	public String getServerName() {
		return "Test Server";
	}

	@Override
	public int getServerPort() {
		return socket.getLocalPort();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getRemoteAddr() {
		return socket.getInetAddress().getHostAddress();
	}

	@Override
	public String getRemoteHost() {
		return socket.getInetAddress().getHostName();
	}

	@Override
	public void setAttribute(final String name, final Object obj) {
		if (obj == null)
			attributes.remove(name);
		else
			attributes.put(name, obj);
	}

	@Override
	public void removeAttribute(final String name) {
		attributes.remove(name);
	}

	@Override
	public Locale getLocale() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Enumeration<Locale> getLocales() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getRemotePort() {
		return socket.getPort();
	}

	@Override
	public String getLocalName() {
		return socket.getLocalAddress().getHostName();
	}

	@Override
	public String getLocalAddr() {
		return socket.getLocalAddress().getHostAddress();
	}

	@Override
	public int getLocalPort() {
		return socket.getLocalPort();
	}

	@Override
	public ServletContext getServletContext() {
		throw new UnsupportedOperationException();
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isAsyncStarted() {
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		return false;
	}

	@Override
	public AsyncContext getAsyncContext() {
		throw new UnsupportedOperationException();
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.REQUEST;
	}

	@Override
	public String getRequestId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getProtocolRequestId() {
		return "";
	}

	@Override
	public ServletConnection getServletConnection() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAuthType() {
		return null;
	}

	@Override
	public Cookie[] getCookies() {
		return new Cookie[0];
	}

	@Override
	public long getDateHeader(final String name) {
		return LocalDateTime.parse(getHeader(name), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
	}

	@Override
	public String getHeader(final String name) {
		final List<String> values = fields.get(new Token(name));
		if (values == null)
			return null;
		return values.getFirst();
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		return Collections.enumeration(fields.get(new Token(name)));
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration(fields.keySet().stream().map(Token::value).toList());
	}

	@Override
	public int getIntHeader(final String name) {
		return Integer.parseInt(getHeader(name));
	}

	@Override
	public String getMethod() {
		return method.token().value();
	}

	@Override
	public String getPathInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPathTranslated() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getContextPath() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getQueryString() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getRemoteUser() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isUserInRole(String role) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Principal getUserPrincipal() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getRequestedSessionId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getRequestURI() {
		throw new UnsupportedOperationException();
	}

	@Override
	public StringBuffer getRequestURL() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getServletPath() {
		throw new UnsupportedOperationException();
	}

	@Override
	public HttpSession getSession(boolean create) {
		throw new UnsupportedOperationException();
	}

	@Override
	public HttpSession getSession() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String changeSessionId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean authenticate(final HttpServletResponse response) throws IOException, ServletException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void login(String username, String password) throws ServletException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void logout() throws ServletException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Part getPart(String name) throws IOException, ServletException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
		throw new UnsupportedOperationException();
	}
}
