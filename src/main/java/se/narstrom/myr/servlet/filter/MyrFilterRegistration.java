package se.narstrom.myr.servlet.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import se.narstrom.myr.servlet.InitParameters;
import se.narstrom.myr.servlet.context.Context;

public final class MyrFilterRegistration implements FilterRegistration.Dynamic {
	private final Context context;

	private final String filterName;

	private final String className;

	private final InitParameters initParameters = new InitParameters();

	private final List<String> servletNameMappings = new ArrayList<>();

	private final AtomicBoolean inited = new AtomicBoolean(false);

	private final Lock initLock = new ReentrantLock();

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
		for (final DispatcherType dispatcherType : dispatcherTypes) {
			for (final String servletName : servletNames) {
				context.addServletNameFilterMapping(dispatcherType, servletName, isMatchAfter, filterName);
				servletNameMappings.add(servletName);
			}
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
		return initParameters.setInitParameter(name, value);
	}

	@Override
	public String getInitParameter(final String name) {
		return initParameters.getInitParameter(name);
	}

	@Override
	public Set<String> setInitParameters(final Map<String, String> params) {
		return initParameters.setInitParameters(params);
	}

	@Override
	public Map<String, String> getInitParameters() {
		return initParameters.getInitParameters();
	}

	@Override
	public void setAsyncSupported(boolean isAsyncSupported) {
		throw new UnsupportedOperationException();
	}

	public void service(final ServletRequest trquest, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		init();
		filter.doFilter(trquest, response, chain);
	}

	public void init() throws IOException, ServletException {
		if (inited.get())
			return;
		initLock.lock();
		try {
			if (inited.get())
				return;
			if (filterClass == null) {
				try {
					@SuppressWarnings("unchecked")
					final Class<? extends Filter> clazz = (Class<? extends Filter>) context.getClassLoader().loadClass(className);
					filterClass = clazz;
				} catch (ClassNotFoundException ex) {
					throw new ServletException(ex);
				}
			}
			if (filter == null)
				filter = context.createFilter(filterClass);
			filter.init(new MyrFilterConfig(context, filterName, initParameters));
			inited.set(true);
		} finally {
			initLock.unlock();
		}
	}
}
