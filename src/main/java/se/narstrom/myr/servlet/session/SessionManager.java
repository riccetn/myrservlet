package se.narstrom.myr.servlet.session;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

public final class SessionManager {
	private final Random random = new SecureRandom();

	private final ConcurrentMap<SessionKey, Session> sessions = new ConcurrentHashMap<>();

	private final List<HttpSessionListener> sessionListeners = new CopyOnWriteArrayList<>();

	private final List<HttpSessionAttributeListener> attributeListeners = new CopyOnWriteArrayList<>();


	public void addSessionListener(final HttpSessionListener listener) {
		sessionListeners.add(listener);
	}

	public void removeSessionListener(final HttpSessionListener listener) {
		sessionListeners.remove(listener);
	}

	private void fireSessionCreated(final HttpSession session) {
		final HttpSessionEvent event = new HttpSessionEvent(session);
		for (final HttpSessionListener listener : sessionListeners) {
			listener.sessionCreated(event);
		}
	}

	private void fireSessionDestroyed(final HttpSession session) {
		final HttpSessionEvent event = new HttpSessionEvent(session);
		for (final HttpSessionListener listener : sessionListeners) {
			listener.sessionDestroyed(event);
		}
	}

	public void addAttributeListener(final HttpSessionAttributeListener listener) {
		attributeListeners.add(listener);
	}

	public void removeAttributeListener(final HttpSessionAttributeListener listener) {
		attributeListeners.remove(listener);
	}

	void fireAttributeAdded(final HttpSession session, final String name, final Object value) {
		final HttpSessionBindingEvent event = new HttpSessionBindingEvent(session, name, value);
		for (final HttpSessionAttributeListener listener : attributeListeners) {
			listener.attributeAdded(event);
		}
	}

	void fireAttributeReplaced(final HttpSession session, final String name, final Object value) {
		final HttpSessionBindingEvent event = new HttpSessionBindingEvent(session, name, value);
		for (final HttpSessionAttributeListener listener : attributeListeners) {
			listener.attributeReplaced(event);
		}
	}

	void fireAttributeRemoved(final HttpSession session, final String name) {
		final HttpSessionBindingEvent event = new HttpSessionBindingEvent(session, name);
		for (final HttpSessionAttributeListener listener : attributeListeners) {
			listener.attributeRemoved(event);
		}
	}

	public Session findSession(final SessionKey key) {
		final Session session = sessions.get(key);

		if (session == null)
			return null;

		final Instant now = Instant.now();

		if (Instant.ofEpochMilli(session.getLastAccessedTime()).plus(Duration.ofSeconds(session.getMaxInactiveInterval())).isBefore(now)) {
			sessions.remove(key, session);
			return null;
		}

		session.setLastAccessTime(now.toEpochMilli());
		return session;
	}

	public Session createSession(final ServletContext context, final String address) {
		final String id = generateSessionId();
		final Session session = new Session(this, context, id);
		sessions.put(new SessionKey(context.getServletContextName(), address, id), session);
		fireSessionCreated(session);
		return session;
	}

	public void invalidateSession(final Session session) {
		if (!sessions.values().remove(session))
			throw new IllegalStateException("Session not registrated with this manager");
		fireSessionDestroyed(session);
	}

	private String generateSessionId() {
		return Long.toHexString(random.nextLong());
	}

	public String changeSessionId(final Session session, final String contextName, final String address) {
		final SessionKey oldKey = new SessionKey(contextName, address, session.getId());
		if (!sessions.remove(oldKey, session))
			throw new IllegalStateException("Session not avaiable under old id");

		final String newSessionId = generateSessionId();
		session.setId(newSessionId);

		final SessionKey newKey = new SessionKey(contextName, address, newSessionId);
		sessions.put(newKey, session);

		return newSessionId;
	}
}
