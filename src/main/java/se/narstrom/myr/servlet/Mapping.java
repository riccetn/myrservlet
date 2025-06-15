package se.narstrom.myr.servlet;

import java.util.Objects;

import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.MappingMatch;

public final class Mapping implements HttpServletMapping {
	private MappingMatch mappingMatch;
	private String pattern;
	private String matchValue;
	private CanonicalizedPath canoicalizedPath;
	private String servletPath;
	private String pathInfo;
	private String servletName;

	public Mapping(
			final MappingMatch mappingMatch,
			final String pattern,
			final String matchValue,
			final CanonicalizedPath canonicalizedPath,
			final String servletPath,
			final String pathInfo,
			final String servletName) {
		this.mappingMatch = Objects.requireNonNull(mappingMatch);
		this.pattern = Objects.requireNonNull(pattern);
		this.matchValue = Objects.requireNonNull(matchValue);
		this.canoicalizedPath = Objects.requireNonNull(canonicalizedPath);
		this.servletPath = Objects.requireNonNull(servletPath);
		this.pathInfo = pathInfo;
		this.servletName = Objects.requireNonNull(servletName);
	}

	@Override
	public MappingMatch getMappingMatch() {
		return mappingMatch;
	}

	public CanonicalizedPath getCanonicalizedPath() {
		return canoicalizedPath;
	}

	@Override
	public String getMatchValue() {
		return matchValue;
	}

	public String getServletPath() {
		return servletPath;
	}

	public String getPathInfo() {
		return pathInfo;
	}

	@Override
	public String getPattern() {
		return pattern;
	}

	@Override
	public String getServletName() {
		return servletName;
	}
}
