package se.narstrom.myr.servlet.filter;

import java.util.Enumeration;

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import se.narstrom.myr.servlet.InitParameters;

public final class MyrFilterConfig implements FilterConfig {
	private final ServletContext context;
	private final String filterName;
	private final InitParameters initParameters;

	public MyrFilterConfig(final ServletContext context, final String filterName, final InitParameters initParameters) {
		this.context = context;
		this.filterName = filterName;
		this.initParameters = initParameters;
	}

	@Override
	public String getFilterName() {
		return filterName;
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
