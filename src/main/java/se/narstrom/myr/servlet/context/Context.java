package se.narstrom.myr.servlet.context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.jasper.servlet.JspServlet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextAttributeListener;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestAttributeEvent;
import jakarta.servlet.ServletRequestAttributeListener;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;
import se.narstrom.myr.http.v1.RequestTarget;
import se.narstrom.myr.servlet.CanonicalizedPath;
import se.narstrom.myr.servlet.InitParameters;
import se.narstrom.myr.servlet.Mapping;
import se.narstrom.myr.servlet.async.AsyncHandler;
import se.narstrom.myr.servlet.attributes.Attributes;
import se.narstrom.myr.servlet.container.Container;
import se.narstrom.myr.servlet.dispatcher.Dispatcher;
import se.narstrom.myr.servlet.request.Request;
import se.narstrom.myr.servlet.response.Response;
import se.narstrom.myr.servlet.servlet.MyrServletRegistration;
import se.narstrom.myr.servlet.session.Session;
import se.narstrom.myr.servlet.session.SessionManager;

// 4. Servlet Context
// ==================
// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#servlet-context
public final class Context implements AutoCloseable, ServletContext {
	private final Logger logger;

	private final Container container;

	private final Path base;

	private final ServletClassLoader classLoader;

	private final SessionManager sessionManager;

	private final Map<Integer, String> errorMappings = new HashMap<>();

	private final Map<String, Charset> localeEncodingMappings = new HashMap<>();

	private final Map<String, String> mimeTypeMappings = new HashMap<>();

	private final Map<String, String> exceptionMappings = new ConcurrentHashMap<>();

	private final Map<DispatcherType, Map<String, List<String>>> servletNameFilterMappings = new EnumMap<>(DispatcherType.class);
	{
		for (final DispatcherType type : DispatcherType.values())
			servletNameFilterMappings.put(type, new HashMap<>());
	}

	private boolean inited = false;

	private final String contextPath;

	private String contextName = "Root Context";

	public Context(final String contextPath, final Path base, final SessionManager sessionManager, final Container container) {
		Objects.requireNonNull(contextPath);
		Objects.requireNonNull(base);
		Objects.requireNonNull(sessionManager);
		Objects.requireNonNull(container);
		if (!contextPath.isEmpty() && !contextPath.startsWith("/"))
			throw new IllegalArgumentException("Invalid contextPath: " + contextPath);
		if (contextPath.length() == 1)
			throw new IllegalArgumentException("Invalid contextPath: " + contextPath);
		if (!contextPath.isEmpty() && contextPath.indexOf('/', 1) != -1)
			throw new IllegalArgumentException("Invalid contextPath: " + contextPath);

		this.contextPath = contextPath;
		this.base = base.toAbsolutePath();
		this.sessionManager = sessionManager;
		this.logger = Logger.getLogger("ServletContext:" + contextPath);
		this.classLoader = new ServletClassLoader(this, base, getClass().getClassLoader());
		this.container = container;
	}

	public void service(final Request request, final Response response) throws IOException {
		final String uri = request.getRequestURI();
		assert uri.startsWith(contextPath);

		final String path = uri.substring(contextPath.length());

		fireRequestInitialized(request);

		final AsyncHandler async = new AsyncHandler(this, path, request, response);
		try {
			async.service();
		} catch (final ServletException | IOException ex) {
			final LogRecord logRecord = new LogRecord(Level.WARNING, "Exception from dispatch in context ''{0}''");
			logRecord.setParameters(new Object[] { contextName });
			logRecord.setThrown(ex);
			logger.log(logRecord);

			if (!response.isCommitted())
				handleException(request, response, ex);
		} finally {
			fireRequestDestroyed(request);
		}
	}

	private void handleException(final Request request, final Response response, final Throwable ex) {
		try {
			final String path = getExceptionMapping(ex);

			if (path != null) {
				getRequestDispatcher(path).error(request, response, ex, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
				return;
			}

			if (ex instanceof ServletException sex) {
				final Throwable cause = sex.getRootCause();
				if (cause != null) {
					handleException(request, response, sex.getRootCause());
					return;
				}
			}

			switch (ex) {
				case UnavailableException uex when !uex.isPermanent() -> handleError(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, null, ex);
				case UnavailableException _ -> handleError(request, response, HttpServletResponse.SC_NOT_FOUND, null, ex);
				case FileNotFoundException _ -> handleError(request, response, HttpServletResponse.SC_NOT_FOUND, null, ex);
				default -> handleError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, ex);
			}
		} catch (final ServletException | IOException ex2) {
			logger.log(Level.SEVERE, "Error in error-page dispatch", ex2);
		}
	}

	public void handleError(final Request request, final Response response, final int status, String message, final Throwable ex) {
		try {
			final String path = getErrorMapping(status);

			if (path == null) {
				if (message == null)
					message = ex.getMessage();
				if (message == null)
					message = "Unknown Error";
				response.reset();
				response.setStatus(status);
				response.setContentType("text/plain");
				response.setContentLength(message.length());
				response.getWriter().write(message);
				response.flushBuffer();
				return;
			}

			getRequestDispatcher(path).error(request, response, ex, status, message);
		} catch (final ServletException | IOException ex2) {
			logger.log(Level.SEVERE, "Error in error-page dispatch", ex2);
		}
	}


	// 4.3 Initialization Parameters
	// =============================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#initialization-parameters
	private final InitParameters initParameters = new InitParameters();

	@Override
	public String getInitParameter(final String name) {
		return initParameters.getInitParameter(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return initParameters.getInitParameterNames();
	}

	@Override
	public boolean setInitParameter(final String name, final String value) {
		if (inited)
			throw new IllegalStateException("Context inited");
		return initParameters.setInitParameter(name, value);
	}


	// 4.4. Configuration Methods
	// =========================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#configuration-methods

	// 4.4.1. Programmatically Adding and Configuring Servlets
	// ======================================================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#programmatically-adding-and-configuring-servlets
	private final ServletRegistry registry = new ServletRegistry(this);

	@Override
	public ServletRegistration.Dynamic addServlet(final String servletName, final String className) {
		return registry.addServlet(servletName, className);
	}

	@Override
	public ServletRegistration.Dynamic addServlet(final String servletName, final Servlet servlet) {
		return registry.addServlet(servletName, servlet);
	}

	@Override
	public ServletRegistration.Dynamic addServlet(final String servletName, final Class<? extends Servlet> servletClass) {
		return registry.addServlet(servletName, servletClass);
	}

	@Override
	public ServletRegistration.Dynamic addJspFile(final String servletName, final String jspFile) {
		final ServletRegistration.Dynamic registration = addServlet(servletName, JspServlet.class);
		registration.setInitParameter("jspFile", jspFile);
		return registration;
	}

	@Override
	public <T extends Servlet> T createServlet(final Class<T> clazz) throws ServletException {
		try {
			return clazz.getConstructor().newInstance();
		} catch (final ReflectiveOperationException ex) {
			throw new ServletException(ex);
		}
	}

	@Override
	public ServletRegistration getServletRegistration(final String servletName) {
		return registry.getServletRegistration(servletName);
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		return registry.getServletRegistrations();
	}


	// 4.4.2. Programmatically Adding and Configuring Filters
	// =====================================================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#programmatically-adding-and-configuring-filters

	@Override
	public FilterRegistration.Dynamic addFilter(final String filterName, final String className) {
		return registry.addFilter(filterName, className);
	}

	@Override
	public FilterRegistration.Dynamic addFilter(final String filterName, final Filter filter) {
		return registry.addFilter(filterName, filter);
	}

	@Override
	public FilterRegistration.Dynamic addFilter(final String filterName, final Class<? extends Filter> filterClass) {
		return registry.addFilter(filterName, filterClass);
	}

	@Override
	public <T extends Filter> T createFilter(final Class<T> clazz) throws ServletException {
		try {
			return clazz.getConstructor().newInstance();
		} catch (final ReflectiveOperationException ex) {
			throw new ServletException(ex);
		}
	}

	@Override
	public FilterRegistration getFilterRegistration(final String filterName) {
		return registry.getFilterRegistration(filterName);
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		return registry.getFilterRegistrations();
	}


	// 4.4.3. Programmatically Adding and Configuring Listeners
	// ========================================================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#programmatically-adding-and-configuring-listeners
	private final List<ServletContextListener> servletContextListeners = new CopyOnWriteArrayList<>();
	private final List<ServletRequestListener> servletRequestListeners = new CopyOnWriteArrayList<>();
	private final List<ServletRequestAttributeListener> servletRequestAttributeListeners = new CopyOnWriteArrayList<>();
	private final List<HttpSessionIdListener> sessionIdListeners = new CopyOnWriteArrayList<>();

	@Override
	@SuppressWarnings("unchecked")
	public void addListener(final String className) {
		final Class<EventListener> clazz;
		try {
			clazz = (Class<EventListener>) classLoader.loadClass(className);
		} catch (final ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
		addListener(clazz);
	}

	@Override
	public <T extends EventListener> void addListener(final T listener) {
		if (listener instanceof ServletContextListener l)
			servletContextListeners.add(l);
		if (listener instanceof ServletContextAttributeListener l)
			attributes.addAttributeListener(new ContextAttributeListener(this, l));
		if (listener instanceof ServletRequestListener l)
			servletRequestListeners.add(l);
		if (listener instanceof ServletRequestAttributeListener l)
			servletRequestAttributeListeners.add(l);
		if (listener instanceof HttpSessionListener l)
			sessionManager.addSessionListener(l);
		if (listener instanceof HttpSessionAttributeListener l)
			sessionManager.addAttributeListener(l);
		if (listener instanceof HttpSessionIdListener l)
			sessionIdListeners.add(l);
	}

	@Override
	public void addListener(Class<? extends EventListener> listenerClass) {
		try {
			addListener(createListener(listenerClass));
		} catch (final ServletException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public <T extends EventListener> T createListener(final Class<T> clazz) throws ServletException {
		try {
			return clazz.getConstructor().newInstance();
		} catch (final ReflectiveOperationException ex) {
			throw new ServletException(ex);
		}
	}

	private void fireSessionIdChanged(final HttpSession session, final String newId) {
		final HttpSessionEvent event = new HttpSessionEvent(session);
		for (final HttpSessionIdListener listener : sessionIdListeners) {
			listener.sessionIdChanged(event, newId);
		}
	}

	private void fireRequestInitialized(final ServletRequest request) {
		final ServletRequestEvent event = new ServletRequestEvent(this, request);
		for (final ServletRequestListener listener : servletRequestListeners) {
			listener.requestInitialized(event);
		}
	}

	private void fireRequestDestroyed(final ServletRequest request) {
		final ServletRequestEvent event = new ServletRequestEvent(this, request);
		for (final ServletRequestListener listener : servletRequestListeners.reversed()) {
			listener.requestDestroyed(event);
		}
	}

	public void fireServletRequestAttributeAdded(final ServletRequest request, final String name, final Object value) {
		final ServletRequestAttributeEvent event = new ServletRequestAttributeEvent(this, request, name, value);
		for (final ServletRequestAttributeListener listener : servletRequestAttributeListeners) {
			listener.attributeAdded(event);
		}
	}

	public void fireServletRequestAttributeReplaced(final ServletRequest request, final String name, final Object value) {
		final ServletRequestAttributeEvent event = new ServletRequestAttributeEvent(this, request, name, value);
		for (final ServletRequestAttributeListener listener : servletRequestAttributeListeners) {
			listener.attributeReplaced(event);
		}
	}

	public void fireServletRequestAttributeRemoved(final ServletRequest request, final String name, final Object value) {
		final ServletRequestAttributeEvent event = new ServletRequestAttributeEvent(this, request, name, value);
		for (final ServletRequestAttributeListener listener : servletRequestAttributeListeners) {
			listener.attributeRemoved(event);
		}
	}


	// 4.4.4. Programmatically Configuring Session Time Out
	// ====================================================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#programmatically-configuring-session-time-out
	@Override
	public int getSessionTimeout() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSessionTimeout(final int sessionTimeout) {
		throw new UnsupportedOperationException();
	}


	// 4.4.5. Programmatically Configuring Character Encoding
	// ======================================================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#programmatically-configuring-character-encoding
	@Override
	public String getRequestCharacterEncoding() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setRequestCharacterEncoding(final String encoding) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getResponseCharacterEncoding() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setResponseCharacterEncoding(final String encoding) {
		throw new UnsupportedOperationException();
	}


	// 4.5. Context Attributes
	// =======================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#context-attributes
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
	public void setAttribute(final String name, final Object object) {
		attributes.setAttribute(name, object);
	}

	@Override
	public void removeAttribute(final String name) {
		attributes.removeAttribute(name);
	}


	// 4.6. Resources
	// ==============
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#resources
	@Override
	public URL getResource(final String path) throws MalformedURLException {
		if (path.charAt(0) != '/')
			throw new MalformedURLException();
		final Path realPath = Paths.get(getRealPath(path));
		if (!Files.exists(realPath))
			return null;
		return realPath.toUri().toURL();
	}

	@Override
	public InputStream getResourceAsStream(final String path) {
		if (path.charAt(0) != '/')
			return null;
		final Path realPath = Paths.get(getRealPath(path));
		try {
			return Files.newInputStream(realPath);
		} catch (IOException _) {
			return null;
		}
	}

	@Override
	public Set<String> getResourcePaths(final String path) {
		final Set<String> result = new HashSet<>();
		final boolean slash = path.charAt(path.length() - 1) == '/';

		final Path realPath = Paths.get(getRealPath(path));
		try (final DirectoryStream<Path> dir = Files.newDirectoryStream(realPath)) {
			for (final Path child : dir) {
				if (slash)
					result.add(path + realPath.relativize(child).toString());
				else
					result.add(path + "/" + realPath.relativize(child).toString());
			}
		} catch (final IOException ex) {
			throw new RuntimeException(ex);
		}
		return result;
	}


	// 7. Sessions
	// ===========
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#sessions
	public SessionManager getSessionManager() {
		return sessionManager;
	}

	public String changeSessionId(final Session session, final String address) {
		final String oldId = session.getId();
		final String newId = sessionManager.changeSessionId(session, contextName, address);
		fireSessionIdChanged(session, oldId);
		return newId;
	}


	public void init() {
		if (inited)
			return;

		logger.info("Initializing servlet context");

		final ServletContextEvent event = new ServletContextEvent(this);
		for (final ServletContextListener listener : servletContextListeners) {
			listener.contextInitialized(event);
		}

		inited = true;
	}

	public void destroy() {
		if (!inited)
			return;
		inited = false;
		logger.info("Destroying servlet context");
		for (final MyrServletRegistration registration : registry.getServletRegistrations().values()) {
			registration.destroy();
		}
	}

	public String getExceptionMapping(final Throwable ex) {
		Class<?> exceptionClass = ex.getClass();
		String path = null;
		while (exceptionClass != Object.class) {
			path = exceptionMappings.get(exceptionClass.getName());
			if (path != null)
				return path;
			exceptionClass = exceptionClass.getSuperclass();
		}
		return null;
	}

	public String getErrorMapping(final int status) {
		return errorMappings.get(status);
	}

	@Override
	public void close() {
		for (final MyrServletRegistration registration : registry.getServletRegistrations().values()) {
			registration.destroy();
		}
	}

	public void addErrorPage(final int errorCode, final String path) {
		this.errorMappings.put(errorCode, path);
	}

	public void addExceptionPage(final String exceptionClassName, final String path) {
		this.exceptionMappings.put(exceptionClassName, path);
	}

	public void addLocaleEncodingMapping(final Locale locale, final Charset encoding) {
		Objects.requireNonNull(locale);
		Objects.requireNonNull(encoding);
		localeEncodingMappings.put(locale.toLanguageTag(), encoding);
	}

	public Charset getLocaleEncoding(final Locale locale) {
		Charset charset = localeEncodingMappings.get(locale.toLanguageTag());
		if(charset == null)
			charset = localeEncodingMappings.get(locale.getLanguage());
		return charset;
	}

	public void addMimeTypeMapping(final String extension, final String mediaType) {
		mimeTypeMappings.put(extension, mediaType);
	}

	public void addServletNameFilterMapping(final DispatcherType dispatcherType, final String servletName, final boolean isMatchAfter, final String filterName) {
		final List<String> mappings = servletNameFilterMappings.get(dispatcherType).computeIfAbsent(servletName, _ -> new ArrayList<>());
		if (isMatchAfter)
			mappings.addLast(filterName);
		else
			mappings.addFirst(filterName);
	}

	@Override
	public String getContextPath() {
		return contextPath;
	}

	@Override
	public ServletContext getContext(final String uripath) {
		return container.getContext(uripath);
	}

	@Override
	public int getMajorVersion() {
		return 6;
	}

	@Override
	public int getMinorVersion() {
		return 1;
	}

	@Override
	public int getEffectiveMajorVersion() {
		return 6;
	}

	@Override
	public int getEffectiveMinorVersion() {
		return 1;
	}

	@Override
	public String getMimeType(final String file) {
		String mediaType = null;
		try {
			final Path path = Paths.get(getRealPath(file));
			final String filename = path.getFileName().toString();
			final int dot = filename.lastIndexOf('.');
			if (dot != -1) {
				final String fileext = filename.substring(dot + 1);
				mediaType = mimeTypeMappings.get(fileext);
			}

			if (mediaType == null)
				mediaType = Files.probeContentType(path);
		} catch (final IOException _) {
			/* Ignore */
		}

		return mediaType;
	}

	@Override
	public Dispatcher getRequestDispatcher(String uri) {
		final RequestTarget target = RequestTarget.parse(uri);
		final CanonicalizedPath canonicalizedPath = CanonicalizedPath.canonicalize(target.absolutePath());
		final Mapping mapping = registry.findServletRegistrationFromUri(canonicalizedPath);

		logger.log(Level.INFO, "Creating Dispatcher for uri {0} to servlet {1}", new Object[] { uri, mapping.getServletName() });

		return new Dispatcher(this, mapping, registry, target.query());
	}

	@Override
	public Dispatcher getNamedDispatcher(final String name) {
		if (registry.getServletRegistration(name) == null)
			return null;
		return new Dispatcher(this, name, registry);
	}

	@Override
	public void log(final String msg) {
		logger.info(msg);
	}

	@Override
	public void log(String message, Throwable throwable) {
		logger.log(Level.SEVERE, message, throwable);
	}

	@Override
	public String getRealPath(String path) {
		Objects.requireNonNull(path);
		if (path.charAt(0) == '/')
			path = path.substring(1);
		return base.resolve(path).toAbsolutePath().toString();
	}

	@Override
	public String getServerInfo() {
		return "myrservlet/0.2";
	}

	@Override
	public String getServletContextName() {
		return contextName;
	}

	public void setServletContextName(final String name) {
		Objects.requireNonNull(name);
		if (inited)
			throw new IllegalStateException("Context already inited");
		this.contextName = name;
	}

	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSessionTrackingModes(final Set<SessionTrackingMode> sessionTrackingModes) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public void declareRoles(String... roleNames) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getVirtualServerName() {
		throw new UnsupportedOperationException();
	}
}
