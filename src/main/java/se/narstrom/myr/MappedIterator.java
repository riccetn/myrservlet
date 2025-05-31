package se.narstrom.myr;

import java.util.Iterator;
import java.util.function.Function;

public final class MappedIterator<R, E> implements Iterator<R> {
	private final Iterator<E> wrapped;

	private final Function<E, R> mapper;

	public MappedIterator(final Iterator<E> wrapped, final Function<E, R> mapper) {
		this.wrapped = wrapped;
		this.mapper = mapper;
	}

	@Override
	public boolean hasNext() {
		return wrapped.hasNext();
	}

	@Override
	public R next() {
		return mapper.apply(wrapped.next());
	}
}
