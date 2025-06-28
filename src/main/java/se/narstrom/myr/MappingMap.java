package se.narstrom.myr;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class MappingMap<KR, K, VR, V> extends AbstractMap<KR, VR> {
	private final Map<K, V> wrapped;
	private final Function<K, KR> keyMapper;
	private final Function<V, VR> valueMapper;

	public MappingMap(final Map<K, V> wrapped, final Function<K, KR> keyMapper, final Function<V, VR> valueMapper) {
		this.wrapped = wrapped;
		this.keyMapper = keyMapper;
		this.valueMapper = valueMapper;
	}

	@Override
	public Set<Entry<KR, VR>> entrySet() {
		return new MappingSet<>(wrapped.entrySet(), ent -> Map.entry(keyMapper.apply(ent.getKey()), valueMapper.apply(ent.getValue())));
	}
}
