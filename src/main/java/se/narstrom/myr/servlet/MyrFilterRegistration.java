package se.narstrom.myr.servlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;

public final class MyrFilterRegistration implements FilterRegistration.Dynamic {
	private final Context context;

	private final String filterName;

	private final String className;

	private final Map<String, String> initParameters = new HashMap<>();

	private final List<String> servletNameMappings = new ArrayList<>();

	private Class<? extends Filter> filterClass;

	private Filter filter;

	public MyrFilterRegistration(final Context context, final String name, final String className) {
		this.context = context;
		this.filterName = name;
		this.className = className;
	}

	public MyrFilterRegistration(final Context context, final String name, final Class<? extends Filter> clazz) {
		this.context = context;
		this.filterName = name;
		this.className = clazz.getName();
		this.filterClass = clazz;
	}

	public MyrFilterRegistration(final Context context, final String name, final Filter filter) {
		this.context = context;
		this.filterName = name;
		final Class<? extends Filter> clazz = filter.getClass();
		this.className = clazz.getName();
		this.filterClass = clazz;
		this.filter = filter;
	}

	@Override
	public void addMappingForServletNames(final EnumSet<DispatcherType> dispatcherTypes, final boolean isMatchAfter, final String... servletNames) {
		for (final String servletName : servletNames) {
			context.addServletNameFilterMapping(servletName, isMatchAfter, filterName);
			servletNameMappings.add(servletName);
		}
	}

	@Override
	public Collection<String> getServletNameMappings() {
		return Collections.unmodifiableCollection(servletNameMappings);
	}

	@Override
	public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... urlPatterns) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<String> getUrlPatternMappings() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		return filterName;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public boolean setInitParameter(final String name, final String value) {
		if (initParameters.containsKey(name))
			return false;
		initParameters.put(name, value);
		return true;
	}

	@Override
	public String getInitParameter(final String name) {
		return initParameters.get(name);
	}

	@Override
	public Set<String> setInitParameters(final Map<String, String> initParameters) {
		final Set<String> successful = new HashSet<>();
		for (final Map.Entry<String, String> initParameter : initParameters.entrySet()) {
			final String name = initParameter.getKey();
			final String value = initParameter.getValue();
			if (setInitParameter(name, value))
				successful.add(name);
		}
		return successful;
	}

	@Override
	public Map<String, String> getInitParameters() {
		return Collections.unmodifiableMap(initParameters);
	}

	@Override
	public void setAsyncSupported(boolean isAsyncSupported) {
		throw new UnsupportedOperationException();
	}

}
