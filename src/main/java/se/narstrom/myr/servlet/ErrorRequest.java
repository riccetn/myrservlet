package se.narstrom.myr.servlet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import se.narstrom.myr.servlet.request.Request;

public final class ErrorRequest extends Request {
	public ErrorRequest(final HttpServletRequest request, final Dispatcher dispatcher) {
		super(request, dispatcher);
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.ERROR;
	}
}
