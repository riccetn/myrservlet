package se.narstrom.myr.servlet.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Enumeration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import se.narstrom.myr.servlet.attributes.AttributeEvent;
import se.narstrom.myr.servlet.attributes.AttributeListener;
import se.narstrom.myr.servlet.attributes.Attributes;

public final class Session implements HttpSession, AttributeListener {
	private final SessionManager manager;

	private final ServletContext context;

	private final Attributes attributes = new Attributes();

	private String sessionId;

	private Instant lastAccessedTime = Instant.now();

	private Duration maxInactiveInterval = Duration.ofMinutes(30);

	public Session(final SessionManager manager, final ServletContext context, final String sessionId) {
		this.manager = manager;
		this.context = context;
		this.sessionId = sessionId;
		this.attributes.addAttributeListener(this);
	}

	@Override
	public long getCreationTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getId() {
		return sessionId;
	}

	@Override
	public long getLastAccessedTime() {
		return lastAccessedTime.toEpochMilli();
	}

	public void setLastAccessTime(final Long milli) {
		this.lastAccessedTime = Instant.ofEpochMilli(milli);
	}

	@Override
	public ServletContext getServletContext() {
		return context;
	}

	@Override
	public void setMaxInactiveInterval(final int interval) {
		this.maxInactiveInterval = Duration.ofSeconds(interval);
	}

	@Override
	public int getMaxInactiveInterval() {
		return (int) this.maxInactiveInterval.toSeconds();
	}

	@Override
	public Object getAttribute(final String name) {
		return attributes.getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return attributes.getAttributeNames();
	}

	@Override
	public void setAttribute(final String name, final Object value) {
		attributes.setAttribute(name, value);
	}

	@Override
	public void removeAttribute(final String name) {
		attributes.removeAttribute(name);
	}

	@Override
	public void invalidate() {
		manager.invalidateSession(this);
	}

	@Override
	public boolean isNew() {
		// TODO: Fix this
		return true;
	}

	void setId(final String sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public void attributeAdded(AttributeEvent event) {
		manager.fireAttributeAdded(this, event.getName(), event.getValue());
	}

	@Override
	public void attributeReplaced(AttributeEvent event) {
		manager.fireAttributeReplaced(this, event.getName(), event.getValue());
	}

	@Override
	public void attributeRemoved(AttributeEvent event) {
		manager.fireAttributeRemoved(this, event.getName());
	}
}
