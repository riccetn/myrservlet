package se.narstrom.myr;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

public final class MappingSet<R, E> extends AbstractSet<R> {
	private final Set<E> wrapped;
	private final Function<E, R> mapper;

	public MappingSet(final Set<E> wrapped, final Function<E, R> mapper) {
		this.wrapped = wrapped;
		this.mapper = mapper;
	}

	@Override
	public int size() {
		return wrapped.size();
	}

	@Override
	public Iterator<R> iterator() {
		return new MappedIterator<>(wrapped.iterator(), mapper);
	}
}
