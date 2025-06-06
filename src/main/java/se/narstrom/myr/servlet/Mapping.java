package se.narstrom.myr.servlet;

import java.util.Objects;

import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.MappingMatch;

public final class Mapping implements HttpServletMapping {
	private MappingMatch mappingMatch;
	private String pattern;
	private String unencodedUri;
	private int splitIndex;
	private String servletName;

	public Mapping(final MappingMatch mappingMatch, final String pattern, final String unencodedUri, final int splitIndex, final String servletName) {
		this.mappingMatch = Objects.requireNonNull(mappingMatch);
		this.pattern = Objects.requireNonNull(pattern);
		this.unencodedUri = Objects.requireNonNull(unencodedUri);
		this.splitIndex = splitIndex;
		this.servletName = Objects.requireNonNull(servletName);
	}

	@Override
	public MappingMatch getMappingMatch() {
		return mappingMatch;
	}

	@Override
	public String getMatchValue() {
		return unencodedUri.substring(0, splitIndex);
	}

	public String getPathInfo() {
		return unencodedUri.substring(splitIndex);
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
