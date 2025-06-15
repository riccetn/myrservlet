package se.narstrom.myr.servlet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public final class ForwardRequest extends HttpServletRequestWrapper {
	private Dispatcher dispatcher;

	public ForwardRequest(final HttpServletRequest request, final Dispatcher dispatcher) {
		super(request);
		this.dispatcher = dispatcher;
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.FORWARD;
	}

	@Override
	public HttpServletMapping getHttpServletMapping() {
		return dispatcher.getMapping();
	}
}
