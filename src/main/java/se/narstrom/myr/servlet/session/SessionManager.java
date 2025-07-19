package se.narstrom.myr.servlet.session;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SessionManager {
	private final Random random = new SecureRandom();

	private final ConcurrentMap<SessionKey, Session> sessions = new ConcurrentHashMap<>();

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

	public Session findOrCreateSession(final SessionKey key) {
		Session session = findSession(key);
		if (session != null)
			return session;
		else
			return createSession(key.contextName(), key.address());
	}

	public Session createSession(final String contextName, final String address) {
		final String id = generateSessionId();
		final Session session = new Session(id);
		sessions.put(new SessionKey(contextName, address, id), session);
		return session;
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
