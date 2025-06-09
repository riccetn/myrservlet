package se.narstrom.myr.servlet.async;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import se.narstrom.myr.servlet.Dispatcher;
import se.narstrom.myr.servlet.request.Request;

public final class AsyncRequest extends Request {
	private final AsyncContext asyncContext = new AsyncHandler();

	public AsyncRequest(final HttpServletRequest request, final Dispatcher dispatcher) {
		super(request, dispatcher);
	}

	@Override
	public AsyncContext getAsyncContext() {
		return asyncContext;
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.ASYNC;
	}
}
