package se.narstrom.myr.util;

public sealed interface Result<T, E extends Exception> {
	record Ok<T, E extends Exception>(T charset) implements Result<T, E> {
		@Override
		public T value() {
			return charset;
		}
	}

	record Error<T, E extends Exception>(E exception) implements Result<T, E> {
		@Override
		public T value() throws E {
			throw exception;
		}
	}

	T value() throws E;
}
