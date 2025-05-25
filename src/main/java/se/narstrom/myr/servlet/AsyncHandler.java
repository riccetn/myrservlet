package se.narstrom.myr.servlet;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class AsyncHandler implements AsyncContext {
	private final Lock lock = new ReentrantLock();

	private final Condition cond = lock.newCondition();

	private HttpServletRequest originalRequest = null;

	private HttpServletResponse originalResponse = null;

	private HttpServletRequest request = null;

	private HttpServletResponse response = null;

	private State state = State.DISPATCHING;

	private Context context = null;

	private String path = null;

	void service(final HttpServletRequest request, final HttpServletResponse response) throws InterruptedException {
		lock.lock();
		try {
			if (state != State.DISPATCHING)
				throw new IllegalStateException();
			this.request = this.originalRequest = request;
			this.response = this.originalResponse = response;
			this.context = (Context) request.getServletContext();

			final String uri = request.getRequestURI();
			final String contextPath = context.getContextPath();

			assert uri.startsWith(contextPath);

			this.path = uri.substring(contextPath.length());

			while (state == State.DISPATCHING) {
				this.state = State.DISPATCHED;

				// TODO: Find some better way of dealing with this flag
				// final Dispatcher dispatcher = context.getRequestDispatcher(path);
				// originalRequest.setAsyncSupported(dispatcher.getRegistration().isAsyncSupported());

				lock.unlock();
				try {
					context.service(this.request, this.response);
				} finally {
					lock.lock();
				}

				state = switch (state) {
					case DISPATCHED, COMPLETING -> State.COMPLETED;
					case ASYNC_STARTED -> State.ASYNC_WAIT;
					case REDISPATCHING -> State.DISPATCHING;
					default -> throw new IllegalStateException();
				};

				while (state == State.ASYNC_WAIT)
					cond.await();
			}

			assert state == State.COMPLETED;
		} finally {
			lock.unlock();
		}
	}

	void startAsync() {
		lock.lock();
		try {
			if (state != State.DISPATCHED)
				throw new IllegalStateException();
			this.state = State.ASYNC_STARTED;
		} finally {
			lock.unlock();
		}
	}

	void startAsync(final ServletRequest request, final ServletResponse response) {
		lock.lock();
		try {
			startAsync();
			this.request = (HttpServletRequest) request;
			this.response = (HttpServletResponse) response;
		} finally {
			lock.unlock();
		}
	}

	boolean isAsyncStarted() {
		lock.lock();
		try {
			return switch (state) {
				case ASYNC_STARTED, REDISPATCHING, COMPLETING -> true;
				default -> false;
			};
		} finally {
			lock.unlock();
		}
	}

	@Override
	public ServletRequest getRequest() {
		lock.lock();
		try {
			return request;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public ServletResponse getResponse() {
		lock.lock();
		try {
			return response;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean hasOriginalRequestAndResponse() {
		lock.lock();
		try {
			return request == originalRequest && response == originalResponse;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void dispatch() {
		final ServletContext context = request.getServletContext();
		final String uri = request.getRequestURI();
		final String contextPath = context.getContextPath();

		assert uri.startsWith(contextPath);

		final String path = uri.substring(contextPath.length());

		dispatch(context, path);
	}

	@Override
	public void dispatch(final String path) {
		dispatch(request.getServletContext(), path);
	}

	@Override
	public void dispatch(final ServletContext context, final String path) {
		lock.lock();
		try {
			state = switch (state) {
				case ASYNC_STARTED -> State.REDISPATCHING;
				case ASYNC_WAIT -> State.DISPATCHING;
				default -> throw new IllegalStateException();
			};
			this.context = (Context) context;
			this.path = path;
			this.request = new AsyncRequest(request, (Context) context, path);
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void complete() {
		lock.lock();
		try {
			state = switch (state) {
				case ASYNC_STARTED -> State.COMPLETING;
				case ASYNC_WAIT -> State.COMPLETED;
				default -> throw new IllegalStateException();
			};
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void start(final Runnable run) {
		Thread.ofVirtual().start(run);
	}

	@Override
	public void addListener(final AsyncListener listener) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public void addListener(final AsyncListener listener, final ServletRequest servletRequest, final ServletResponse servletResponse) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public <T extends AsyncListener> T createListener(final Class<T> clazz) throws ServletException {
		try {
			return clazz.getConstructor().newInstance();
		} catch (final ReflectiveOperationException ex) {
			throw new ServletException(ex);
		}
	}

	@Override
	public void setTimeout(final long timeout) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public long getTimeout() {
		throw new UnsupportedOperationException("Not implemented");
	}

	public enum State {
		DISPATCHING, DISPATCHED, ASYNC_STARTED, REDISPATCHING, COMPLETING, ASYNC_WAIT, COMPLETED
	}
}
