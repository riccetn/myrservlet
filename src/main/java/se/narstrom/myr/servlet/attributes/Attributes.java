package se.narstrom.myr.servlet.attributes;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#attributes
// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#context-attributes
public final class Attributes {
	private final ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();

	private final List<AttributeListener> listeners = new CopyOnWriteArrayList<>();

	public Object getAttribute(final String name) {
		return map.get(name);
	}

	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(map.keySet());
	}

	public void setAttribute(final String name, final Object value) {
		if (value == null) {
			removeAttribute(name);
			return;
		}

		final Object oldValue = map.put(name, value);
		if (oldValue == null)
			fireAttributeAdded(name, value);
		else if (oldValue != value)
			fireAttributeReplaced(name, oldValue);
	}

	public void removeAttribute(final String name) {
		final Object oldValue = map.remove(name);
		if (oldValue != null)
			fireAttributeRemoved(name, oldValue);
	}

	public void addAttributeListener(final AttributeListener listener) {
		listeners.add(listener);
	}

	private void fireAttributeAdded(final String name, final Object value) {
		final AttributeEvent event = new AttributeEvent(this, name, value);
		for (final AttributeListener listener : listeners) {
			listener.attributeAdded(event);
		}
	}

	private void fireAttributeReplaced(final String name, final Object value) {
		final AttributeEvent event = new AttributeEvent(this, name, value);
		for (final AttributeListener listener : listeners) {
			listener.attributeReplaced(event);
		}
	}

	private void fireAttributeRemoved(final String name, final Object value) {
		final AttributeEvent event = new AttributeEvent(this, name, value);
		for (final AttributeListener listener : listeners) {
			listener.attributeRemoved(event);
		}
	}
}
