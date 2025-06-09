package se.narstrom.myr.servlet.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Enumeration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;

public final class Session implements HttpSession {

	private final String sessionId;

	private Instant lastAccessedTime = Instant.now();

	private Duration maxInactiveInterval = Duration.ofMinutes(30);

	public Session(final String sessionId) {
		this.sessionId = sessionId;
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
		throw new UnsupportedOperationException();
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
	public Object getAttribute(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAttribute(String name, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeAttribute(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void invalidate() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isNew() {
		throw new UnsupportedOperationException();
	}
}
