package se.narstrom.myr.servlet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletSecurityElement;

public final class Registration implements ServletRegistration.Dynamic {
	private final Context context;

	private final String name;

	private final String className;

	private final Map<String, String> initParameters = new HashMap<>();

	private Servlet servlet;

	private boolean inited = false;

	private boolean asyncSupported = false;

	public Registration(final Context context, final String name, final String className) {
		this.context = context;
		this.name = name;
		this.className = className;
	}

	public Registration(final Context context, final String name, final String className, final Servlet servlet) {
		this(context, name, className);
		this.servlet = servlet;
	}

	void init() throws ServletException, ClassNotFoundException {
		if (inited)
			return;

		if (servlet == null) {
			@SuppressWarnings("unchecked")
			final Class<? extends Servlet> clazz = (Class<? extends Servlet>) context.getClassLoader().loadClass(className);
			servlet = context.createServlet(clazz);
		}

		servlet.init(new Config(context, initParameters));
		inited = true;
	}

	void destroy() {
		if(!inited)
			return;
		inited = false;
		servlet.destroy();
	}

	Servlet getServlet() {
		return servlet;
	}

	boolean isAsyncSupported() {
		return asyncSupported;
	}

	@Override
	public Set<String> addMapping(final String... urlPatterns) {
		final HashSet<String> ret = new HashSet<>();
		for (final String pattern : urlPatterns) {
			if (!context.addMapping(pattern, name))
				ret.add(pattern);
		}
		return ret;
	}

	@Override
	public Collection<String> getMappings() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getRunAsRole() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getClassName() {
		return servlet.getClass().getName();
	}

	@Override
	public boolean setInitParameter(final String name, final String value) {
		Objects.requireNonNull(name);
		final String oldValue = initParameters.put(name, value);
		return !Objects.equals(value, oldValue);
	}

	@Override
	public String getInitParameter(final String name) {
		return initParameters.get(name);
	}

	@Override
	public Set<String> setInitParameters(final Map<String, String> initParameters) {
		final Set<String> ret = new HashSet<>();
		for (final Map.Entry<String, String> entry : initParameters.entrySet()) {
			if (initParameters.containsKey(entry.getKey()))
				ret.add(entry.getKey());
			else
				this.initParameters.put(entry.getKey(), entry.getValue());
		}
		return ret;
	}

	@Override
	public Map<String, String> getInitParameters() {
		return Map.copyOf(initParameters);
	}

	@Override
	public void setAsyncSupported(final boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	@Override
	public void setLoadOnStartup(final int loadOnStartup) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> setServletSecurity(final ServletSecurityElement constraint) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMultipartConfig(final MultipartConfigElement multipartConfig) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setRunAsRole(final String roleName) {
		throw new UnsupportedOperationException();
	}
}
