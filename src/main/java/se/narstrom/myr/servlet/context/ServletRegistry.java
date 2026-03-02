package se.narstrom.myr.servlet.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.MappingMatch;
import se.narstrom.myr.servlet.CanonicalizedPath;
import se.narstrom.myr.servlet.Mapping;
import se.narstrom.myr.servlet.servlet.MyrServletRegistration;

public final class ServletRegistry {
	private static final Logger logger = Logger.getLogger("SerletRegistry");

	private final Context context;
	private final Map<String, MyrServletRegistration> registrations = new ConcurrentHashMap<>();
	private final AtomicReference<String> defaultMapping = new AtomicReference<>();
	private final Map<String, String> exactMappings = new ConcurrentHashMap<>();
	private final Map<String, String> pathMappings = new ConcurrentHashMap<>();
	private final Map<String, String> extentionMappings = new ConcurrentHashMap<>();

	public ServletRegistry(final Context context) {
		this.context = context;
	}

	public MyrServletRegistration addServlet(final String servletName, final String className) {
		final MyrServletRegistration registration = new MyrServletRegistration(this, servletName, className);
		if (registrations.putIfAbsent(servletName, registration) != null)
			return null;
		return registration;
	}

	public MyrServletRegistration addServlet(final String servletName, final Class<? extends Servlet> servletClass) {
		final MyrServletRegistration registration = new MyrServletRegistration(this, servletName, servletClass);
		if (registrations.putIfAbsent(servletName, registration) != null)
			return null;
		return registration;
	}

	public ServletRegistration.Dynamic addServlet(final String servletName, final Servlet servlet) {
		final MyrServletRegistration registration = new MyrServletRegistration(this, servletName, servlet);
		if (registrations.putIfAbsent(servletName, registration) != null)
			return null;
		return registration;
	}

	public boolean addMapping(final String pattern, final String name) {
		if (pattern.startsWith("/") && pattern.endsWith("/*")) {
			final String path = pattern.substring(0, pattern.length() - 2);
			return pathMappings.putIfAbsent(path, name) == null;
		}

		if (pattern.startsWith("*.")) {
			final String extention = pattern.substring(2);
			return extentionMappings.putIfAbsent(extention, name) == null;
		}

		if (pattern.equals("/")) {
			return defaultMapping.compareAndSet(null, name);
		}

		return exactMappings.putIfAbsent(pattern, name) == null;
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

	public MyrServletRegistration getServletRegistration(final String name) {
		return registrations.get(name);
	}

	public Map<String, MyrServletRegistration> getServletRegistrations() {
		return Map.copyOf(registrations);
	}

	public Context getContext() {
		return this.context;
	}
}
