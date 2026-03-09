package se.narstrom.myr.servlet.context;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.MappingMatch;
import se.narstrom.myr.servlet.CanonicalizedPath;
import se.narstrom.myr.servlet.Mapping;
import se.narstrom.myr.servlet.filter.ExecutableFilterChain;
import se.narstrom.myr.servlet.filter.MyrFilterRegistration;
import se.narstrom.myr.servlet.servlet.MyrServletRegistration;

public final class ServletRegistry {
	private static final Logger logger = Logger.getLogger("SerletRegistry");

	private final Context context;

	private final Map<String, MyrServletRegistration> servletRegistrations = new ConcurrentHashMap<>();
	private final AtomicReference<String> defaultMapping = new AtomicReference<>();
	private final Map<String, String> exactMappings = new ConcurrentHashMap<>();
	private final Map<String, String> pathMappings = new ConcurrentHashMap<>();
	private final Map<String, String> extentionMappings = new ConcurrentHashMap<>();

	private record ServletNameFilterMappingKey(String servletName, DispatcherType dispatcherType) {
	}

	private record UrlPatternFilterMapping(UrlPattern uriPattern, String filterName) {
	}

	private final Map<String, MyrFilterRegistration> filterRegistrations = new ConcurrentHashMap<>();
	private final Map<ServletNameFilterMappingKey, List<String>> servletNameFilterMappings = new ConcurrentHashMap<>();
	private final Map<DispatcherType, List<UrlPatternFilterMapping>> urlPatternFilterMappings = new EnumMap<>(DispatcherType.class);
	{
		for (final DispatcherType type : DispatcherType.values()) {
			urlPatternFilterMappings.put(type, new CopyOnWriteArrayList<>());
		}
	}

	public ServletRegistry(final Context context) {
		this.context = context;
	}

	public MyrServletRegistration addServlet(final String servletName, final String className) {
		final MyrServletRegistration registration = new MyrServletRegistration(this, servletName, className);
		if (servletRegistrations.putIfAbsent(servletName, registration) != null)
			return null;
		return registration;
	}

	public MyrServletRegistration addServlet(final String servletName, final Class<? extends Servlet> servletClass) {
		final MyrServletRegistration registration = new MyrServletRegistration(this, servletName, servletClass);
		if (servletRegistrations.putIfAbsent(servletName, registration) != null)
			return null;
		return registration;
	}

	public ServletRegistration.Dynamic addServlet(final String servletName, final Servlet servlet) {
		final MyrServletRegistration registration = new MyrServletRegistration(this, servletName, servlet);
		if (servletRegistrations.putIfAbsent(servletName, registration) != null)
			return null;
		return registration;
	}

	public boolean addServletMapping(final String pattern, final String name) {
		final UrlPattern urlPattern = UrlPattern.parse(pattern);
		return switch (urlPattern) {
			case UrlPattern.ContextRoot _ -> exactMappings.putIfAbsent("", name) == null;
			case UrlPattern.Default _ -> defaultMapping.compareAndSet(null, name);
			case UrlPattern.Exact exact -> exactMappings.putIfAbsent(exact.uri(), name) == null;
			case UrlPattern.Extension extension -> extentionMappings.putIfAbsent(extension.extension(), name) == null;
			case UrlPattern.Path path -> pathMappings.putIfAbsent(path.path(), name) == null;
		};
	}

	public MyrServletRegistration getServletRegistration(final String name) {
		return servletRegistrations.get(name);
	}

	public Map<String, MyrServletRegistration> getServletRegistrations() {
		return Map.copyOf(servletRegistrations);
	}

	public MyrFilterRegistration addFilter(final String filterName, final String className) {
		final MyrFilterRegistration registration = new MyrFilterRegistration(this, filterName, className);
		if (filterRegistrations.putIfAbsent(filterName, registration) != null)
			return null;
		return registration;
	}

	public MyrFilterRegistration addFilter(final String filterName, final Filter filter) {
		final MyrFilterRegistration registration = new MyrFilterRegistration(this, filterName, filter);
		if (filterRegistrations.putIfAbsent(filterName, registration) != null)
			return null;
		return registration;
	}

	public MyrFilterRegistration addFilter(final String filterName, final Class<? extends Filter> filterClass) {
		final MyrFilterRegistration registration = new MyrFilterRegistration(this, filterName, filterClass);
		if (filterRegistrations.putIfAbsent(filterName, registration) != null)
			return null;
		return registration;
	}

	public void addFilterMappingForServletName(final DispatcherType dispatcherType, final String servletName, final boolean isMatchAfter, final String filterName) {
		final List<String> filterNames = servletNameFilterMappings.computeIfAbsent(new ServletNameFilterMappingKey(servletName, dispatcherType), _ -> new CopyOnWriteArrayList<>());
		if (isMatchAfter)
			filterNames.addLast(filterName);
		else
			filterNames.addFirst(filterName);
	}

	public void addFilterMappingForUrlPattern(final DispatcherType dispatcherType, final String urlPattern, final boolean isMatchAfter, final String filterName) {
		final List<UrlPatternFilterMapping> mappings = urlPatternFilterMappings.get(dispatcherType);
		final UrlPatternFilterMapping mapping = new UrlPatternFilterMapping(UrlPattern.parse(urlPattern), filterName);
		if (isMatchAfter)
			mappings.addLast(mapping);
		else
			mappings.addFirst(mapping);
	}

	public MyrFilterRegistration getFilterRegistration(final String filterName) {
		return filterRegistrations.get(filterName);
	}

	public Map<String, MyrFilterRegistration> getFilterRegistrations() {
		return Map.copyOf(filterRegistrations);
	}

	public Context getContext() {
		return this.context;
	}

	public ExecutableFilterChain createFilterChain(final DispatcherType dispatcherType, final Mapping mapping) {
		final List<MyrFilterRegistration> filters = new ArrayList<>();
		for (final UrlPatternFilterMapping filterMapping : urlPatternFilterMappings.get(dispatcherType)) {
			if (filterMapping.uriPattern.matches(mapping.getCanonicalizedPath().toString()))
				filters.add(filterRegistrations.get(filterMapping.filterName()));
		}

		final List<String> mappings = servletNameFilterMappings.get(new ServletNameFilterMappingKey(mapping.getServletName(), dispatcherType));
		if (mappings != null) {
			for (final String filterName : mappings) {
				filters.add(filterRegistrations.get(filterName));
			}
		}

		return new ExecutableFilterChain(filters, servletRegistrations.get(mapping.getServletName()));
	}

	public ExecutableFilterChain createFilterChain(final DispatcherType dispatcherType, final String servletName) {
		final List<MyrFilterRegistration> filters = new ArrayList<>();

		final List<String> mappings = servletNameFilterMappings.get(new ServletNameFilterMappingKey(servletName, dispatcherType));
		if (mappings != null) {
			for (final String filterName : mappings) {
				filters.add(filterRegistrations.get(filterName));
			}
		}

		return new ExecutableFilterChain(filters, servletRegistrations.get(servletName));
	}

	public Mapping findServletRegistrationFromUri(final CanonicalizedPath canonicalizedPath) {
		final String uri = canonicalizedPath.toString();

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
			servletName = defaultMapping.get();
			mapping = new Mapping(MappingMatch.DEFAULT, "/", "", canonicalizedPath, uri, uri, servletName);
		}

		if (servletName == null) {
			logger.log(Level.WARNING, "No servlet found for {0}", uri);
			throw new RuntimeException("No Servlet");
		}

		return mapping;
	}
}
