package se.narstrom.myr.servlet.session;

import java.util.Objects;

public record SessionKey(String contextName, String address, String id) {
	public SessionKey {
		Objects.requireNonNull(contextName);
		Objects.requireNonNull(address);
		Objects.requireNonNull(id);
	}
}
