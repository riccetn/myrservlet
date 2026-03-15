package se.narstrom.myr.servlet.dispatcher;

import jakarta.servlet.DispatcherType;
import se.narstrom.myr.servlet.request.Request;

public final class ErrorRequest extends DispatcherHttpRequest {
	public ErrorRequest(final Request request, final Dispatcher dispatcher) {
		super(request, dispatcher);
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.ERROR;
	}
}
