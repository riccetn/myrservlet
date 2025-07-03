package se.narstrom.myr.servlet.dispatcher;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;

public final class ForwardRequest extends DispatcherHttpRequest {
	public ForwardRequest(final HttpServletRequest request, final Dispatcher dispatcher) {
		super(request, dispatcher);
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.FORWARD;
	}
}
