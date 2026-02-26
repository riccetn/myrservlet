package se.narstrom.myr.servlet.servlet;

import java.util.Enumeration;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import se.narstrom.myr.servlet.InitParameters;

public final class MyrServletConfig implements ServletConfig {
	private final ServletContext context;

	private final String servletName;

	private final InitParameters initParameters;

	public MyrServletConfig(final ServletContext context, final String servletName, final InitParameters initParameters) {
		this.context = context;
		this.servletName = servletName;
		this.initParameters = initParameters;
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
		return initParameters.getInitParameter(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return initParameters.getInitParameterNames();
	}
}
