package se.narstrom.myr.servlet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public final class ForwardRequest extends HttpServletRequestWrapper {
	public ForwardRequest(final HttpServletRequest request) {
		super(request);
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.FORWARD;
	}
}
