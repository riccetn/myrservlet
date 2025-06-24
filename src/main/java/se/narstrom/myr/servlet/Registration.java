package se.narstrom.myr.servlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletSecurityElement;
import se.narstrom.myr.servlet.context.Context;

public final class Registration implements ServletRegistration.Dynamic {
	private final Context context;

	private final String name;

	private final String className;

	private final InitParameters initParameters = new InitParameters();

	private final List<String> mappings = new ArrayList<>();

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

		servlet.init(new Config(context, name, initParameters));
		inited = true;
	}

	public void destroy() {
		if (!inited)
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
			else
				mappings.add(pattern);
		}
		return ret;
	}

	@Override
	public Collection<String> getMappings() {
		return Collections.unmodifiableCollection(mappings);
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
		return initParameters.setInitParameter(name, value);
	}

	@Override
	public String getInitParameter(final String name) {
		return initParameters.getInitParameter(name);
	}

	@Override
	public Set<String> setInitParameters(final Map<String, String> parameters) {
		return this.initParameters.setInitParameters(parameters);
	}

	@Override
	public Map<String, String> getInitParameters() {
		return initParameters.getInitParameters();
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
