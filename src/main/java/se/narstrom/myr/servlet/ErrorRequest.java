package se.narstrom.myr.servlet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequestWrapper;
import se.narstrom.myr.servlet.request.Request;

public final class ErrorRequest extends HttpServletRequestWrapper {
	public ErrorRequest(final Request request) {
		super(request);
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.ERROR;
	}
}
