package se.narstrom.myr.servlet.dispatcher;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public final class IncludeRequest extends HttpServletRequestWrapper {
	public IncludeRequest(final HttpServletRequest request) {
		super(request);
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.INCLUDE;
	}
}
