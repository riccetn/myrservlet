package se.narstrom.myr.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
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
import se.narstrom.myr.http.HttpRequest;
import se.narstrom.myr.http.cookie.CookieParser;
import se.narstrom.myr.mime.MediaType;
import se.narstrom.myr.uri.UrlEncoding;
import se.narstrom.myr.util.Result;

public final class MyrRequest implements HttpServletRequest {
	private final Logger logger = Logger.getLogger(getClass().getName());

	private final HttpRequest httpReqeust;

	private final Map<String, Object> attributes = new HashMap<>();

	private boolean inputStreamReturned = false;

	private Context context;

	private Map<String, List<String>> parameters = null;

	private BufferedReader reader = null;

	private Result<Charset, UnsupportedEncodingException> encoding;

	private List<Locale> locales = null;

	private Session session;

	public MyrRequest(final HttpRequest httpRequest) {
		this.httpReqeust = httpRequest;
	}

	void setContext(final Context context) {
		this.context = context;
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
		maybeInitCharacterEncoding();

		return switch (encoding) {
			case Result.Ok(Charset set) when set == null -> null;
			case Result.Ok(Charset set) -> set.name();
			case Result.Error(UnsupportedEncodingException ex) -> ex.getMessage();
		};
	}

	@Override
	public void setCharacterEncoding(final String encoding) throws UnsupportedEncodingException {
		if (reader != null || parameters != null)
			return;
		try {
			this.encoding = new Result.Ok<>(Charset.forName(encoding));
		} catch (final IllegalCharsetNameException | UnsupportedCharsetException ex) {
			throw new UnsupportedEncodingException(ex.toString());
		}
	}

	@Override
	public void setCharacterEncoding(final Charset encoding) {
		if (reader != null || parameters != null)
			return;
		this.encoding = new Result.Ok<>(encoding);
	}

	private void maybeInitCharacterEncoding() {
		if (encoding == null) {
			final String contentType = getContentType();
			if (contentType != null) {
				final String charset = MediaType.parse(contentType).parameters().get("charset");
				if (charset != null) {
					try {
						encoding = new Result.Ok<>(Charset.forName(charset));
					} catch (final IllegalCharsetNameException | UnsupportedCharsetException ex) {
						logger.log(Level.WARNING, "Failed to get charset from content-type header field", ex);
						encoding = new Result.Error<>(new UnsupportedEncodingException(charset));
					}
				}
			}

			if (encoding == null)
				encoding = new Result.Ok<>(null);
		}
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
		if (reader != null)
			throw new IllegalStateException("stream or reader, not both");
		inputStreamReturned = true;
		return httpReqeust.getInputStream();
	}

	@Override
	public String getParameter(String name) {
		maybeInitParameters();
		final List<String> values = parameters.get(name);
		if (values == null)
			return null;
		return values.getFirst();
	}

	@Override
	public Enumeration<String> getParameterNames() {
		maybeInitParameters();
		return Collections.enumeration(parameters.keySet());
	}

	@Override
	public String[] getParameterValues(final String name) {
		maybeInitParameters();
		final List<String> values = parameters.get(name);
		if (values == null)
			return null;
		return values.toArray(String[]::new);
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

		final String query = getQueryString();
		if (query != null) {
			final Map<String, List<String>> queryParameters = UrlEncoding.parse(query);
			for (final Map.Entry<String, List<String>> ent : queryParameters.entrySet()) {
				parameters.computeIfAbsent(ent.getKey(), _ -> new ArrayList<>()).addAll(ent.getValue());
			}
		}

		final String contentType = getContentType();
		MediaType mediaType = null;
		if (contentType != null) {
			mediaType = MediaType.parse(contentType);
		}

		if (mediaType != null && mediaType.type().equals("application") && mediaType.subtype().equals("x-www-form-urlencoded")) {
			getCharacterEncoding();

			byte[] contentBytes = null;
			try {
				contentBytes = getInputStream().readAllBytes();
				if (contentBytes != null) {
					final String content = new String(contentBytes, encoding.value());
					final Map<String, List<String>> contentParams = UrlEncoding.parse(content);
					for (final Map.Entry<String, List<String>> ent : contentParams.entrySet()) {
						parameters.computeIfAbsent(ent.getKey(), _ -> new ArrayList<>()).addAll(ent.getValue());
					}
				}
			} catch (final IOException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	@Override
	public String getProtocol() {
		return httpReqeust.getProtocol();
	}

	@Override
	public String getScheme() {
		return httpReqeust.getScheme();
	}

	@Override
	public String getServerName() {
		return httpReqeust.getServerName();
	}

	@Override
	public int getServerPort() {
		return httpReqeust.getLocalPort();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		if (reader == null) {
			if (inputStreamReturned)
				throw new IllegalStateException("stream or reader, not both");

			maybeInitCharacterEncoding();

			Charset charset = encoding.value();
			if (charset == null)
				charset = Charset.defaultCharset();

			reader = new BufferedReader(new InputStreamReader(getInputStream(), charset));
		}
		return reader;
	}

	@Override
	public String getRemoteAddr() {
		return httpReqeust.getRemoteAddr();
	}

	@Override
	public String getRemoteHost() {
		return httpReqeust.getRemoteHost();
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
		maybeInitLocales();
		return locales.getFirst();
	}

	@Override
	public Enumeration<Locale> getLocales() {
		maybeInitLocales();
		return Collections.enumeration(locales);
	}

	private void maybeInitLocales() {
		if (locales != null)
			return;

		final String acceptLanguage = getHeader("accept-language");
		if (acceptLanguage != null) {
			final List<LanguageRange> ranges = LanguageRange.parse(acceptLanguage);
			if (!ranges.isEmpty())
				locales = ranges.stream().map(range -> Locale.forLanguageTag(range.getRange())).toList();
		}

		if (locales == null)
			locales = List.of(Locale.getDefault());
	}

	@Override
	public boolean isSecure() {
		return httpReqeust.isSecure();
	}

	@Override
	public RequestDispatcher getRequestDispatcher(final String path) {
		if (!path.isEmpty() && path.charAt(0) != '/')
			throw new UnsupportedOperationException("Relative path dispatcher");
		return getServletContext().getRequestDispatcher(path);
	}

	@Override
	public int getRemotePort() {
		return httpReqeust.getRemotePort();
	}

	@Override
	public String getLocalName() {
		return httpReqeust.getLocalName();
	}

	@Override
	public String getLocalAddr() {
		return httpReqeust.getLocalAddr();
	}

	@Override
	public int getLocalPort() {
		return httpReqeust.getLocalPort();
	}

	@Override
	public Context getServletContext() {
		return context;
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		throw new IllegalStateException("Async not supported in this context");
	}

	@Override
	public AsyncContext startAsync(final ServletRequest servletRequest, final ServletResponse servletResponse) throws IllegalStateException {
		throw new IllegalStateException("Async not supported in this context");
	}

	@Override
	public boolean isAsyncStarted() {
		throw new IllegalStateException("Async not supported in this context");
	}

	@Override
	public boolean isAsyncSupported() {
		throw new IllegalStateException("Async not supported in this context");
	}

	@Override
	public AsyncHandler getAsyncContext() {
		throw new IllegalStateException("Async not supported in this context");
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
		final List<Cookie> cookies = new ArrayList<>();
		final Enumeration<String> cookieFields = getHeaders("cookie");
		if (cookieFields != null) {
			while (cookieFields.hasMoreElements()) {
				final String cookieString = cookieFields.nextElement();
				cookies.addAll(CookieParser.parse(cookieString));
			}
		}
		if(cookies.isEmpty())
			return null;
		return cookies.toArray(Cookie[]::new);
	}

	@Override
	public long getDateHeader(final String name) {
		final String dateString = getHeader(name);
		if (dateString == null)
			return -1L;
		return LocalDateTime.parse(dateString, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
	}

	@Override
	public String getHeader(final String name) {
		final Enumeration<String> values = getHeaders(name);
		if (values != null && values.hasMoreElements())
			return values.nextElement();
		return null;
	}

	@Override
	public Enumeration<String> getHeaders(final String name) {
		final List<String> values = httpReqeust.getHeaderFields().get(name.toLowerCase());
		if (values != null)
			return Collections.enumeration(values);
		return Collections.emptyEnumeration();
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		final Set<String> names = httpReqeust.getHeaderFields().keySet();
		return Collections.enumeration(names);
	}

	@Override
	public int getIntHeader(final String name) {
		final String value = getHeader(name);
		if (value == null)
			return -1;
		return Integer.parseInt(value);
	}

	@Override
	public String getMethod() {
		return httpReqeust.getMethod();
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
		return getServletContext().getContextPath();
	}

	@Override
	public String getQueryString() {
		return httpReqeust.getQueryString();
	}

	@Override
	public String getRemoteUser() {
		// TODO: maybeAuthenticate();
		return null;
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
		return httpReqeust.getRequestURI();
	}

	@Override
	public StringBuffer getRequestURL() {
		final StringBuffer sb = new StringBuffer();
		final String scheme = getScheme();
		sb.append(scheme);
		sb.append("://");
		sb.append(getServerName());
		sb.append(":");
		sb.append(Integer.toString(getServerPort()));
		sb.append(getRequestURI());
		return sb;
	}

	@Override
	public String getServletPath() {
		return "";
	}

	@Override
	public HttpSession getSession(boolean create) {
		maybeInitSession(create);
		return session;
	}

	@Override
	public HttpSession getSession() {
		return getSession(true);
	}

	@Override
	public String changeSessionId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		// TODO: maybeInitSession();
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		// TODO: maybeInitSession();
		return false;
	}

	private void maybeInitSession(boolean create) {
		if (session == null && create)
			session = new Session();
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
