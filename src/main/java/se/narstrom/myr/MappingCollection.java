package se.narstrom.myr;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

public final class MappingCollection<R, E> extends AbstractCollection<R> {
	private final Collection<E> wrapped;

	private final Function<E, R> mapper;

	public MappingCollection(final Collection<E> wrapped, final Function<E, R> mapper) {
		this.wrapped = wrapped;
		this.mapper = mapper;
	}

	@Override
	public Iterator<R> iterator() {
		return new MappedIterator<>(this.wrapped.iterator(), mapper);
	}

	@Override
	public int size() {
		return wrapped.size();
	}

}
