package se.narstrom.myr.servlet.attributes;

import java.util.EventListener;

public interface AttributeListener extends EventListener {
	void attributeAdded(AttributeEvent event);
	void attributeRemoved(AttributeEvent event);
	void attributeReplaced(AttributeEvent event);
}
