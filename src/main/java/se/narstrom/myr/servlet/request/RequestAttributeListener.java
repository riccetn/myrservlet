package se.narstrom.myr.servlet.request;

import jakarta.servlet.ServletRequest;
import se.narstrom.myr.servlet.attributes.AttributeEvent;
import se.narstrom.myr.servlet.attributes.AttributeListener;
import se.narstrom.myr.servlet.context.Context;

public final class RequestAttributeListener implements AttributeListener {
	private final Context context;

	private final ServletRequest request;

	public RequestAttributeListener(final Context context, final ServletRequest request) {
		this.context = context;
		this.request = request;
	}

	@Override
	public void attributeAdded(final AttributeEvent event) {
		context.fireServletRequestAttributeAdded(request, event.getName(), event.getValue());
	}

	@Override
	public void attributeReplaced(final AttributeEvent event) {
		context.fireServletRequestAttributeReplaced(request, event.getName(), event.getValue());
	}

	@Override
	public void attributeRemoved(final AttributeEvent event) {
		context.fireServletRequestAttributeRemoved(request, event.getName(), event.getValue());
	}
}
