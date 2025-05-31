package se.narstrom.myr.servlet;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#initialization-parameters
public final class InitParameters {
	private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

	public String getInitParameter(final String name) {
		Objects.requireNonNull(name);
		return map.get(name);
	}

	public Enumeration<String> getInitParameterNames() {
		return Collections.enumeration(map.keySet());
	}

	public boolean setInitParameter(final String name, final String value) {
		Objects.requireNonNull(name);
		return map.putIfAbsent(name, value) != null;
	}
}
