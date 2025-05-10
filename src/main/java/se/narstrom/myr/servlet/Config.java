package se.narstrom.myr.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

public final class Config implements ServletConfig {
	private final ServletContext context;

	private final Map<String, String> initParameters;

	public Config(final ServletContext context, final Map<String, String> initParameters) {
		this.context = context;
		this.initParameters = Map.copyOf(initParameters);
	}

	@Override
	public String getServletName() {
		return "Default";
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
