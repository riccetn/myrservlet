package se.narstrom.myr.servlet.context;

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
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.http.MappingMatch;
import se.narstrom.myr.http.v1.RequestTarget;
import se.narstrom.myr.servlet.Attributes;
import se.narstrom.myr.servlet.CanonicalizedPath;
import se.narstrom.myr.servlet.InitParameters;
import se.narstrom.myr.servlet.Mapping;
import se.narstrom.myr.servlet.MyrFilterRegistration;
import se.narstrom.myr.servlet.Registration;
import se.narstrom.myr.servlet.dispatcher.Dispatcher;
import se.narstrom.myr.servlet.session.SessionManager;
import se.narstrom.myr.uri.Query;

// 4. Servlet Context
// ==================
// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#servlet-context
public final class Context implements AutoCloseable, ServletContext {
	private final Logger logger;

	private final Path base;

	private final ServletClassLoader classLoader;

	private final SessionManager sessionManager;

	private String defaultServlet = null;

	private final Map<String, String> extentionMappings = new HashMap<>();

	private final Map<String, String> pathMappings = new HashMap<>();

	private final Map<String, String> exactMappings = new HashMap<>();

	private final Map<String, String> exceptionMappings = new HashMap<>();

	private final Map<Integer, String> errorMappings = new HashMap<>();

	private final Map<Locale, Charset> localeEncodingMappings = new HashMap<>();

	private final Map<String, String> mimeTypeMappings = new HashMap<>();

	private final Map<String, List<String>> servletNameFilterMappings = new HashMap<>();

	private boolean inited = false;

	private final String contextPath;

	private String contextName = "Root Context";

	public Context(final String contextPath, final Path base, final SessionManager sessionManager) {
		Objects.requireNonNull(contextPath);
		Objects.requireNonNull(base);
		Objects.requireNonNull(sessionManager);
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
	private final Map<String, Registration> registrations = new HashMap<>();

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
	public ServletRegistration.Dynamic addJspFile(final String servletName, final String jspFile) {
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

	public boolean addMapping(final String pattern, final String name) {
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


	// 4.4.2. Programmatically Adding and Configuring Filters
	// =====================================================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#programmatically-adding-and-configuring-filters
	private final Map<String, MyrFilterRegistration> filterRegistrations = new HashMap<>();

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


	// 4.4.3. Programmatically Adding and Configuring Listeners
	// ========================================================
	// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#programmatically-adding-and-configuring-listeners
	private final List<ServletContextListener> servletContextListeners = new ArrayList<>();

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
	public <T extends EventListener> T createListener(final Class<T> clazz) throws ServletException {
		try {
			return clazz.getConstructor().newInstance();
		} catch (final ReflectiveOperationException ex) {
			throw new ServletException(ex);
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
		for (final Registration registration : registrations.values()) {
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
		localeEncodingMappings.put(locale, encoding);
	}

	public Charset getLocaleEncoding(final Locale locale) {
		return localeEncodingMappings.get(locale);
	}

	public void addMimeTypeMapping(final String extension, final String mediaType) {
		mimeTypeMappings.put(extension, mediaType);
	}

	public void addServletNameFilterMapping(final String servletName, final boolean isMatchAfter, final String filterName) {
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
	public Dispatcher getRequestDispatcher(String uri) {
		final RequestTarget target = RequestTarget.parse(uri);
		final CanonicalizedPath canonicalizedPath = CanonicalizedPath.canonicalize(target.absolutePath());
		uri = canonicalizedPath.toString();

		String servletName = null;
		Mapping mapping = null;

		servletName = exactMappings.get(uri);
		if (servletName != null)
			mapping = new Mapping(MappingMatch.EXACT, uri, uri.substring(1), canonicalizedPath, uri, "", servletName);

		if (servletName == null) {
			assert uri.charAt(0) == '/';

			String path = uri;
			if (path.charAt(path.length() - 1) == '/')
				path = path.substring(0, path.length() - 1);

			while (!path.isEmpty()) {
				servletName = pathMappings.get(path);
				if (servletName != null) {
					final String matchValue;
					if (path.length() == uri.length())
						matchValue = "";
					else
						matchValue = uri.substring(path.length() + 1);

					mapping = new Mapping(MappingMatch.PATH, path + "/*", matchValue, canonicalizedPath, path, uri.substring(path.length()), servletName);
					break;
				}
				final int slash = path.lastIndexOf('/');
				path = path.substring(0, slash);
			}

			if (servletName == null) {
				servletName = pathMappings.get("");
				if (servletName != null) {
					mapping = new Mapping(MappingMatch.PATH, "/*", uri.substring(1), canonicalizedPath, "", uri, servletName);
				}
			}
		}

		if (servletName == null) {
			final int slash = uri.lastIndexOf('/');
			final int dot = uri.lastIndexOf('.');
			if (slash < dot) {
				final String extension = uri.substring(dot + 1);
				servletName = extentionMappings.get(extension);
				if (servletName != null) {
					mapping = new Mapping(MappingMatch.EXTENSION, "*." + extension, uri.substring(1, dot), canonicalizedPath, uri.substring(0, slash), uri.substring(slash), servletName);
				}
			}
		}

		if (servletName == null) {
			servletName = defaultServlet;
			mapping = new Mapping(MappingMatch.DEFAULT, "/", "", canonicalizedPath, uri, uri, servletName);
		}

		if (servletName == null) {
			logger.log(Level.WARNING, "No servlet found for {0}", uri);
			throw new RuntimeException("No Servlet");
		}

		logger.log(Level.INFO, "Creating Dispatcher for uri {0} to servlet {1}", new Object[] { uri, servletName });

		return new Dispatcher(this, mapping, registrations.get(servletName), target.query());
	}

	@Override
	public Dispatcher getNamedDispatcher(final String name) {
		final Registration registration = registrations.get(name);
		if (registration == null)
			return null;
		else
			return new Dispatcher(this, null, registrations.get(name), new Query(""))	;
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
