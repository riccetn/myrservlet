package se.narstrom.myr.servlet;

import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#attributes
// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#context-attributes
public final class Attributes {
	private final ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();

	public Object getAttribute(final String name) {
		return map.get(name);
	}

	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(map.keySet());
	}

	public void setAttribute(final String name, final Object obj) {
		if (obj == null)
			map.remove(name);
		else
			map.put(name, obj);
	}

	public void removeAttribute(final String name) {
		map.remove(name);
	}
}
