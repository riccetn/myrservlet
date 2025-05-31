package se.narstrom.myr.http.semantics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import se.narstrom.myr.MappingCollection;

public class Fields {
	private final Map<FieldName, List<FieldValue>> map;

	public Fields(final List<Field> fields) {
		Objects.requireNonNull(fields);

		final Map<FieldName, List<FieldValue>> fieldMap = new HashMap<>();
		for (final Field field : fields) {
			fieldMap.computeIfAbsent(field.name(), _ -> new ArrayList<>()).add(field.value());
		}

		@SuppressWarnings("unchecked")
		final Map.Entry<FieldName, List<FieldValue>>[] entries = new Map.Entry[fieldMap.size()];

		int i = 0;
		for (final Map.Entry<FieldName, List<FieldValue>> entry : fieldMap.entrySet()) {
			entries[i++] = Map.entry(entry.getKey(), List.copyOf(entry.getValue()));
		}

		this.map = Map.ofEntries(entries);
	}

	public String getField(final String name) {
		Objects.requireNonNull(name);

		final List<FieldValue> values = map.get(new FieldName(name));
		if (values == null || values.isEmpty())
			return null;

		return values.getFirst().toString();
	}

	public Enumeration<String> getFields(final String name) {
		Objects.requireNonNull(name);

		final List<FieldValue> values = map.get(new FieldName(name));
		if (values == null || values.isEmpty())
			return Collections.emptyEnumeration();

		final Collection<String> stringValues = new MappingCollection<>(values, Object::toString);
		return Collections.enumeration(stringValues);
	}

	public Enumeration<String> getFieldNames() {
		final Collection<String> stringKeys = new MappingCollection<>(map.keySet(), Object::toString);
		return Collections.enumeration(stringKeys);
	}
}
