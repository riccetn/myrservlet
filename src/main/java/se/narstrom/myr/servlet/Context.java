package se.narstrom.myr.servlet;

import jakarta.servlet.*;
import jakarta.servlet.descriptor.JspConfigDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Context implements AutoCloseable, ServletContext {
	private final Logger logger;

	private final Path base;

	private final Map<String, Object> attributes = new HashMap<>();

	private final Map<String, String> initParameters;

	private final Servlet defaultServlet;

	public Context(final Path base, final Map<String, String> initParameters, final Servlet defaultServlet) {
		this.base = base.toAbsolutePath();
		this.logger = Logger.getLogger("ServletContext:" + this.base);
		this.initParameters = Map.copyOf(initParameters);
		this.defaultServlet = defaultServlet;
	}

	public void init() throws ServletException {
		logger.info(() -> "Initializing servlet context");
		defaultServlet.init(new Config(this));
	}

	@Override
	public void close() {
		defaultServlet.destroy();
	}

	public void service(final MyrServletRequest request, final ServletResponse response) throws ServletException, IOException {
		request.setContext(this);
		defaultServlet.service(request, response);
	}

	@Override
	public String getContextPath() {
		return "";
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
			final Path path = base.resolve(file);
			return Files.probeContentType(path);
		} catch (IOException _) {
			return "application/octet-stream";
		}
	}

	@Override
	public Set<String> getResourcePaths(final String path) {
		return Set.of(base.resolve(path).toString());
	}

	@Override
	public URL getResource(final String path) throws MalformedURLException {
		return base.resolve(path).toUri().toURL();
	}

	@Override
	public InputStream getResourceAsStream(final String path) {
		try {
			return Files.newInputStream(base.resolve(path));
		} catch (IOException _) {
			return null;
		}
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RequestDispatcher getNamedDispatcher(String name) {
		throw new UnsupportedOperationException();
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
	public String getRealPath(final String path) {
		return base.resolve(path).toAbsolutePath().toString();
	}

	@Override
	public String getServerInfo() {
		return "Myr Servlet Container/1.0";
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
		throw new IllegalStateException("Context is already initialized");
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
		throw new UnsupportedOperationException();
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, String className) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ServletRegistration getServletRegistration(String servletName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		throw new UnsupportedOperationException();
	}

	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, String className) {
		throw new UnsupportedOperationException();
	}

	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
		throw new UnsupportedOperationException();
	}

	@Override
	public FilterRegistration getFilterRegistration(String filterName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		throw new UnsupportedOperationException();
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
	public void addListener(final String className) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends EventListener> void addListener(T t) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addListener(Class<? extends EventListener> listenerClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ClassLoader getClassLoader() {
		throw new UnsupportedOperationException();
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
