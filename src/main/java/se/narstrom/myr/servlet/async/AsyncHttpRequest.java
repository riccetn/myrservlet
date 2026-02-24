package se.narstrom.myr.servlet.async;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import se.narstrom.myr.servlet.dispatcher.Dispatcher;
import se.narstrom.myr.servlet.dispatcher.DispatcherHttpRequest;

public final class AsyncHttpRequest extends DispatcherHttpRequest {
	public AsyncHttpRequest(final HttpServletRequest request, final Dispatcher dispatcher) {
		super(request, dispatcher);
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.ASYNC;
	}
}
