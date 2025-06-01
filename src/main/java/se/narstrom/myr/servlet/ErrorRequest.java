package se.narstrom.myr.servlet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;

public final class ErrorRequest extends Request {
	public ErrorRequest(final HttpServletRequest request, final Context context) {
		super(request, context);
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.ERROR;
	}
}
