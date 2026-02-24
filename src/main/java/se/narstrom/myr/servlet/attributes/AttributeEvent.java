package se.narstrom.myr.servlet.attributes;

import java.io.Serial;
import java.util.EventObject;

public final class AttributeEvent extends EventObject {
	@Serial
	private static final long serialVersionUID = 1L;

	private final String name;
	private final Object value;

	public AttributeEvent(final Object source, final String name, final Object value) {
		super(source);
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}
}
