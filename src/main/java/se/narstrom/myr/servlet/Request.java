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
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import se.narstrom.myr.http.cookie.CookieParser;
import se.narstrom.myr.mime.MediaType;
import se.narstrom.myr.util.Result;

// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#the-request
public class Request extends HttpServletRequestWrapper {
	private final Logger logger = Logger.getLogger(getClass().getName());

	private Context context;

	public Request(final HttpServletRequest request, final Context context) {
		super(request);
		this.context = context;
	}


	// 3.1. HTTP Protocol Parameters
	// =============================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#http-protocol-parameters
	private Parameters parameters = null;

	@Override
	public String getParameter(final String name) {
		Objects.requireNonNull(name);
		maybeInitParameters();
		return parameters.getParameter(name);
	}

	@Override
	public Enumeration<String> getParameterNames() {
		maybeInitParameters();
		return parameters.getParameterNames();
	}

	@Override
	public String[] getParameterValues(final String name) {
		Objects.requireNonNull(name);
		maybeInitParameters();
		return parameters.getParameterValues(name);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		maybeInitParameters();
		return parameters.getParameterMap();
	}

	private void maybeInitParameters() {
		if (parameters != null)
			return;

		final String contentTypeString = getContentType();
		final MediaType contentType;
		if (contentTypeString == null)
			contentType = null;
		else
			contentType = MediaType.parse(contentTypeString);

		if (contentType != null && contentType.type().equals("application") && contentType.type().equals("x-www-form-urlencoded")) {
			try {
				parameters = Parameters.parseUrlEncoded(getQueryString(), getReader());
			} catch (final IOException ex) {
				throw new IllegalStateException(ex);
			}
		} else {
			parameters = Parameters.parseQueryOnly(getQueryString());
		}
	}


	// 3.3. Attributes
	// ==========
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#attributes
	private final Attributes attributes = new Attributes();

	@Override
	public Object getAttribute(final String name) {
		return attributes.getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return attributes.getAttributeNames();
	}

	@Override
	public void setAttribute(final String name, final Object obj) {
		attributes.setAttribute(name, obj);
	}

	@Override
	public void removeAttribute(final String name) {
		attributes.removeAttribute(name);
	}


	// 3.4. Headers
	// ============
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#headers
	@Override
	public long getDateHeader(final String name) {
		final String dateString = getHeader(name);
		if (dateString == null)
			return -1L;
		return LocalDateTime.parse(dateString, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
	}

	@Override
	public int getIntHeader(final String name) {
		final String value = getHeader(name);
		if (value == null)
			return -1;
		return Integer.parseInt(value);
	}


	// 3.5. Request URI Path Processing
	// ================================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#request-uri-path-processing
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


	// 3.6. Request Path Elements
	// ==========================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#request-path-elements
	@Override
	public String getContextPath() {
		return getServletContext().getContextPath();
	}

	@Override
	public String getServletPath() {
		return "";
	}

	@Override
	public String getPathInfo() {
		throw new UnsupportedOperationException();
	}


	// 3.7. Path Translation Methods
	// =============================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#request-uri-path-processing
	@Override
	public String getPathTranslated() {
		return context.getRealPath(getPathInfo());
	}


	// 3.10. Cookies
	// =============
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#cookies
	private List<Cookie> cookies = null;

	@Override
	public Cookie[] getCookies() {
		maybeInitCookies();
		if (cookies.isEmpty())
			return null; // NOSONAR: We are required to return null by specification
		return cookies.toArray(Cookie[]::new);
	}

	private void maybeInitCookies() {
		if (cookies != null)
			return;

		final List<Cookie> list = new ArrayList<>();
		final Enumeration<String> fields = getHeaders("cookie");
		if (fields != null) {
			while (fields.hasMoreElements()) {
				final String cookieString = fields.nextElement();
				list.addAll(CookieParser.parse(cookieString));
			}
		}

		this.cookies = list;
	}


	// 3.12 Internationalization
	// =========================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#internationalization
	private List<Locale> locales = null;

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


	// 3.13. Request Data Encoding
	// ===========================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#request-data-encoding
	private Result<Charset, UnsupportedEncodingException> encoding = null;

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
		if (encoding != null)
			return;

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


	// 7. Sessions
	// ===========
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#sessions
	private Session session = null;

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
	public String getRequestedSessionId() {
		throw new UnsupportedOperationException();
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


	// Stream and Reader
	private BufferedReader reader = null;

	private boolean streamReturned;

	private boolean readerReturned;

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (readerReturned)
			throw new IllegalStateException("Stream or reader but not both");
		streamReturned = true;
		return super.getInputStream();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		if (streamReturned)
			throw new IllegalStateException("Stream or reader but not both");
		readerReturned = true;
		if (reader == null) {
			maybeInitCharacterEncoding();

			Charset charset = encoding.value();
			if (charset == null)
				charset = Charset.defaultCharset();

			reader = new BufferedReader(new InputStreamReader(super.getInputStream(), charset));
		}
		return reader;
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
	public RequestDispatcher getRequestDispatcher(final String path) {
		if (!path.isEmpty() && path.charAt(0) != '/')
			throw new UnsupportedOperationException("Relative path dispatcher");
		return getServletContext().getRequestDispatcher(path);
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
