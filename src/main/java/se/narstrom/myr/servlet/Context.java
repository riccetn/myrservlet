package se.narstrom.myr.servlet;

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
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import se.narstrom.myr.http.v1.RequestTarget;

public final class Context implements AutoCloseable, ServletContext {
	private final Logger logger;

	private final Path base;

	private final ServletClassLoader classLoader;

	private final Map<String, Object> attributes = new HashMap<>();

	private final Map<String, String> initParameters = new HashMap<>();

	private final Map<String, Registration> registrations = new HashMap<>();

	private final Map<String, MyrFilterRegistration> filterRegistrations = new HashMap<>();

	private String defaultServlet = null;

	private final Map<String, String> extentionMappings = new HashMap<>();

	private final Map<String, String> pathMappings = new HashMap<>();

	private final Map<String, String> exactMappings = new HashMap<>();

	private final Map<String, String> exceptionMappings = new HashMap<>();

	private final Map<Integer, String> errorMappings = new HashMap<>();

	private final Map<Locale, Charset> localeEncodingMappings = new HashMap<>();

	private final Map<String, String> mimeTypeMappings = new HashMap<>();

	private final List<ServletContextListener> servletContextListeners = new ArrayList<>();

	private final Map<String, List<String>> servletNameFilterMappings = new HashMap<>();

	private boolean inited = false;

	private final String contextPath;

	private String contextName = "Root Context";

	public Context(final String contextPath, final Path base) {
		Objects.requireNonNull(contextPath);
		Objects.requireNonNull(base);
		if (!contextPath.isEmpty() && !contextPath.startsWith("/"))
			throw new IllegalArgumentException("Invalid contextPath: " + contextPath);
		if (contextPath.length() == 1)
			throw new IllegalArgumentException("Invalid contextPath: " + contextPath);
		if (!contextPath.isEmpty() && contextPath.indexOf('/', 1) != -1)
			throw new IllegalArgumentException("Invalid contextPath: " + contextPath);

		this.contextPath = contextPath;
		this.base = base.toAbsolutePath();
		this.logger = Logger.getLogger("ServletContext:" + contextPath);
		this.classLoader = new ServletClassLoader(this, base, getClass().getClassLoader());
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
		for (final Registration registration : registrations.values()) {
			registration.destroy();
		}
	}

	void service(final HttpServletRequest request, final HttpServletResponse response) {
		final String uri = request.getRequestURI();
		if (!uri.startsWith(contextPath))
			throw new IllegalArgumentException("This request is not for this context: " + uri + " is not in " + contextPath);

		String path = uri.substring(contextPath.length());
		final String query = request.getQueryString();
		if (query != null)
			path += "?" + query;

		final Dispatcher dispatcher = getRequestDispatcher(path);

		try {
			dispatcher.request(request, response);
		} catch (final ServletException | IOException ex) {
			final LogRecord logRecord = new LogRecord(Level.WARNING, "Exception from dispatch in context ''{0}''");
			logRecord.setParameters(new Object[] { contextName });
			logRecord.setThrown(ex);
			logger.log(logRecord);

			if (!response.isCommitted())
				handleException(request, response, ex);
		}
	}

	void handleException(HttpServletRequest request, final HttpServletResponse response, final Throwable ex) {
		try {
			Class<?> exceptionClass = ex.getClass();
			String path = null;
			while (exceptionClass != Object.class) {
				path = exceptionMappings.get(exceptionClass.getName());
				if (path != null)
					break;
				exceptionClass = exceptionClass.getSuperclass();
			}

			if (path != null) {
				getRequestDispatcher(path).error(request, response, ex, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}

			switch (ex) {
				case UnavailableException uex when !uex.isPermanent() -> handleError(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service Temporary Unavailable");
				case UnavailableException _ -> handleError(request, response, HttpServletResponse.SC_NOT_FOUND, "Not Found");
				case ServletException sex -> handleException(request, response, sex.getRootCause());
				case FileNotFoundException _ -> handleError(request, response, HttpServletResponse.SC_NOT_FOUND, "Not Found");
				default -> response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
			}
		} catch (final ServletException | IOException ex2) {
			logger.log(Level.SEVERE, "Error in error-page dispatch", ex2);
		}
	}

	void handleError(final HttpServletRequest request, final HttpServletResponse response, final int status, final String message) {
		try {
			final String path = errorMappings.get(status);

			if (path == null) {
				response.sendError(status, message);
				return;
			}

			response.setStatus(status);
			getRequestDispatcher(path).request(request, response);
		} catch (final ServletException | IOException ex) {
			logger.log(Level.SEVERE, "Error in error-page dispatch", ex);
		}
	}

	@Override
	public void close() {
		for (final Registration registration : registrations.values()) {
			registration.destroy();
		}
	}

	boolean addMapping(final String pattern, final String name) {
		if (pattern.startsWith("/") && pattern.endsWith("/*")) {
			final String path = pattern.substring(0, pattern.length() - 2);
			if (pathMappings.containsKey(path))
				return false;
			pathMappings.put(path, name);
			return true;
		}

		if (pattern.startsWith("*.")) {
			final String extention = pattern.substring(2);
			if (extentionMappings.containsKey(extention))
				return false;
			extentionMappings.put(extention, name);
			return true;
		}

		if (pattern.equals("")) {
			throw new UnsupportedOperationException("context-root mapping not implemented");
		}

		if (pattern.equals("/")) {
			if (defaultServlet != null)
				return false;
			defaultServlet = name;
			return true;
		}

		if (exactMappings.containsKey(pattern))
			return false;
		exactMappings.put(pattern, name);
		return true;
	}

	void addErrorPage(final int errorCode, final String path) {
		this.errorMappings.put(errorCode, path);
	}

	void addExceptionPage(final String exceptionClassName, final String path) {
		this.exceptionMappings.put(exceptionClassName, path);
	}

	void addLocaleEncodingMapping(final Locale locale, final Charset encoding) {
		Objects.requireNonNull(locale);
		Objects.requireNonNull(encoding);
		localeEncodingMappings.put(locale, encoding);
	}

	Charset getLocaleEncoding(final Locale locale) {
		return localeEncodingMappings.get(locale);
	}

	void addMimeTypeMapping(final String extension, final String mediaType) {
		mimeTypeMappings.put(extension, mediaType);
	}

	void addServletNameFilterMapping(final String servletName, final boolean isMatchAfter, final String filterName) {
		final List<String> mappings = servletNameFilterMappings.computeIfAbsent(servletName, _ -> new ArrayList<>());
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
		return null;
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
		try {
			final Path path = Paths.get(getRealPath(file));
			final String filename = path.getFileName().toString();
			final int dot = filename.lastIndexOf('.');
			if (dot != -1) {
				final String fileext = filename.substring(dot + 1);
				final String mediaType = mimeTypeMappings.get(fileext);
				if (mediaType != null)
					return mediaType;
			}
			return Files.probeContentType(path);
		} catch (IOException _) {
			return "application/octet-stream";
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
		try {
			return Files.newInputStream(base.resolve(path.substring(1)));
		} catch (IOException _) {
			return null;
		}
	}

	@Override
	public Dispatcher getRequestDispatcher(String uri) {
		final RequestTarget target = RequestTarget.parse(uri);
		final CanonicalizedPath canonicalizedPath = CanonicalizedPath.canonicalize(target.absolutePath());
		uri = canonicalizedPath.toString();

		String servletName = null;
		servletName = exactMappings.get(uri);

		if (servletName == null) {
			assert uri.charAt(0) == '/';

			String path = uri;
			if (path.charAt(path.length() - 1) == '/')
				path = path.substring(0, path.length() - 1);

			while (!path.isEmpty()) {
				servletName = pathMappings.get(path);
				if (servletName == null)
					break;
				final int slash = path.lastIndexOf('/');
				path = path.substring(0, slash);
			}
		}

		if (servletName == null) {
			final int slash = uri.lastIndexOf('/');
			final int dot = uri.lastIndexOf('.');
			if (slash < dot) {
				servletName = extentionMappings.get(uri.substring(dot + 1));
			}
		}

		if (servletName == null)
			servletName = defaultServlet;

		if (servletName == null) {
			logger.log(Level.WARNING, "No servlet found for {0}", uri);
			throw new RuntimeException("No Servlet");
		}

		logger.log(Level.INFO, "Creating Dispatcher for uri {0} to servlet {1}", new Object[] { uri, servletName });

		return getNamedDispatcher(servletName);
	}

	@Override
	public Dispatcher getNamedDispatcher(final String name) {
		final Registration registration = registrations.get(name);
		if (registration == null)
			return null;
		else
			return new Dispatcher(this, registrations.get(name));
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
		if (path.charAt(0) == '/')
			path = path.substring(1);
		return base.resolve(path).toAbsolutePath().toString();
	}

	@Override
	public String getServerInfo() {
		return "myrservlet/0.2";
	}

	@Override
	public String getInitParameter(final String name) {
		return initParameters.get(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return Collections.enumeration(initParameters.keySet());
	}

	@Override
	public boolean setInitParameter(final String name, final String value) {
		Objects.requireNonNull(name);
		if (inited)
			throw new IllegalStateException("Context inited");
		final String oldValue = initParameters.put(name, value);
		return !Objects.equals(value, oldValue);
	}

	@Override
	public Object getAttribute(final String name) {
		return attributes.get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(attributes.keySet());
	}

	@Override
	public void setAttribute(final String name, final Object object) {
		if (object == null)
			attributes.remove(name);
		else
			attributes.put(name, object);
	}

	@Override
	public void removeAttribute(final String name) {
		attributes.remove(name);
	}

	@Override
	public String getServletContextName() {
		return contextName;
	}

	void setServletContextName(final String name) {
		Objects.requireNonNull(name);
		if (inited)
			throw new IllegalStateException("Context already inited");
		this.contextName = name;
	}

	@Override
	public ServletRegistration.Dynamic addServlet(final String servletName, final String className) {
		try {
			@SuppressWarnings("unchecked")
			final Class<? extends Servlet> clazz = (Class<? extends Servlet>) classLoader.loadClass(className);
			return addServlet(servletName, clazz);
		} catch (final ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public ServletRegistration.Dynamic addServlet(final String servletName, final Servlet servlet) {
		if (registrations.containsKey(servletName))
			return null;
		final Registration registration = new Registration(this, servletName, servlet.getClass().getName(), servlet);
		registrations.put(servletName, registration);
		return registration;
	}

	@Override
	public ServletRegistration.Dynamic addServlet(final String servletName, final Class<? extends Servlet> servletClass) {
		try {
			return addServlet(servletName, createServlet(servletClass));
		} catch (ServletException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
		throw new UnsupportedOperationException();
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
		return registrations.get(servletName);
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		return Collections.unmodifiableMap(registrations);
	}

	@Override
	public FilterRegistration.Dynamic addFilter(final String filterName, final String className) {
		if (filterRegistrations.containsKey(filterName))
			return null;
		final MyrFilterRegistration registration = new MyrFilterRegistration(this, filterName, className);
		filterRegistrations.put(filterName, registration);
		return registration;
	}

	@Override
	public FilterRegistration.Dynamic addFilter(final String filterName, final Filter filter) {
		if (filterRegistrations.containsKey(filterName))
			return null;
		final MyrFilterRegistration registration = new MyrFilterRegistration(this, filterName, filter);
		filterRegistrations.put(filterName, registration);
		return registration;
	}

	@Override
	public FilterRegistration.Dynamic addFilter(final String filterName, final Class<? extends Filter> filterClass) {
		if (filterRegistrations.containsKey(filterName))
			return null;
		final MyrFilterRegistration registration = new MyrFilterRegistration(this, filterName, filterClass);
		filterRegistrations.put(filterName, registration);
		return registration;
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
		return filterRegistrations.get(filterName);
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		return Collections.unmodifiableMap(filterRegistrations);
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
		switch (listener) {
			case ServletContextListener l -> servletContextListeners.add(l);
			default -> {
			}
		}
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
	public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
		try {
			return clazz.getConstructor().newInstance();
		} catch (final ReflectiveOperationException ex) {
			throw new ServletException(ex);
		}
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

	@Override
	public int getSessionTimeout() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSessionTimeout(int sessionTimeout) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getRequestCharacterEncoding() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setRequestCharacterEncoding(String encoding) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getResponseCharacterEncoding() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setResponseCharacterEncoding(String encoding) {
		throw new UnsupportedOperationException();
	}
}
