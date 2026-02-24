package se.narstrom.myr.servlet.async;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletRequest;
import se.narstrom.myr.servlet.dispatcher.Dispatcher;
import se.narstrom.myr.servlet.dispatcher.DispatcherRequest;

public final class AsyncRequest extends DispatcherRequest {
	public AsyncRequest(final ServletRequest request, final Dispatcher dispatcher) {
		super(request, dispatcher);
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.ASYNC;
	}
}
