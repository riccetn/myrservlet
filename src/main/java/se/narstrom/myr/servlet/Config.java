package se.narstrom.myr.servlet;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

public final class Config implements ServletConfig {
	private final ServletContext context;

	private String servletName;

	private final Map<String, String> initParameters;

	public Config(final ServletContext context, final String servletName, final Map<String, String> initParameters) {
		this.context = context;
		this.servletName = servletName;
		this.initParameters = Map.copyOf(initParameters);
	}

	@Override
	public String getServletName() {
		return servletName;
	}

	@Override
	public ServletContext getServletContext() {
		return context;
	}

	@Override
	public String getInitParameter(final String name) {
		return initParameters.get(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return Collections.enumeration(initParameters.keySet());
	}
}
