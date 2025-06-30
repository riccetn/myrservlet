package se.narstrom.myr.servlet.async;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import se.narstrom.myr.servlet.Dispatcher;

public final class AsyncRequest extends HttpServletRequestWrapper {
	private final Dispatcher dispatcher;

	public AsyncRequest(final HttpServletRequest request, final Dispatcher dispatcher) {
		super(request);
		this.dispatcher = dispatcher;
	}

	@Override
	public HttpServletMapping getHttpServletMapping() {
		return dispatcher.getMapping();
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.ASYNC;
	}
}
