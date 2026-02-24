package se.narstrom.myr.servlet.context;

import jakarta.servlet.ServletContextAttributeEvent;
import jakarta.servlet.ServletContextAttributeListener;
import se.narstrom.myr.servlet.attributes.AttributeEvent;
import se.narstrom.myr.servlet.attributes.AttributeListener;

final class ContextAttributeListener implements AttributeListener {
	private final Context context;
	private final ServletContextAttributeListener listener;

	ContextAttributeListener(final Context context, final ServletContextAttributeListener listener) {
		this.context = context;
		this.listener = listener;
	}

	@Override
	public void attributeAdded(final AttributeEvent event) {
		listener.attributeAdded(new ServletContextAttributeEvent(context, event.getName(), event.getValue()));
	}

	@Override
	public void attributeRemoved(final AttributeEvent event) {
		listener.attributeRemoved(new ServletContextAttributeEvent(context, event.getName(), event.getValue()));
	}

	@Override
	public void attributeReplaced(final AttributeEvent event) {
		listener.attributeReplaced(new ServletContextAttributeEvent(context, event.getName(), event.getValue()));
	}
}
