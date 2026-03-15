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

	@Override
	public String getServletPath() {
		return dispatcher.getMapping().getServletPath();
	}

	@Override
	public String getPathInfo() {
		return dispatcher.getMapping().getPathInfo();
	}

	@Override
	public String getPathTranslated() {
		final String pathInfo = getPathInfo();
		if (pathInfo == null)
			return null;
		return dispatcher.getContext().getRealPath(pathInfo);
	}
}
