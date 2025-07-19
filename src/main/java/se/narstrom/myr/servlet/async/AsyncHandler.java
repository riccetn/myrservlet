package se.narstrom.myr.servlet.async;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import se.narstrom.myr.servlet.dispatcher.Dispatcher;
import se.narstrom.myr.servlet.request.Request;
import se.narstrom.myr.servlet.response.Response;

public final class AsyncHandler implements AsyncContext {
	private final Logger logger = Logger.getLogger("AsyncHandler");

	private final Lock lock = new ReentrantLock();
	private final Condition cond = lock.newCondition();

	private final List<AsyncListener> listeners = new CopyOnWriteArrayList<>();

	private AsyncState state = AsyncState.DISPATCHING;

	private ServletContext context;
	private String path;

	private final Request originalRequest;
	private final Response originalResponse;

	private ServletRequest currentRequest;
	private ServletResponse currentResponse;

	private long timeout = 30_000L;

	public AsyncHandler(final ServletContext context, final String path, final Request request, final Response response) {
		this.context = context;
		this.path = path;
		this.originalRequest = request;
		this.originalResponse = response;
		this.currentRequest = request;
		this.currentResponse = response;
	}

	public void service() throws ServletException, IOException {
		this.originalRequest.setAsyncHandler(this);

		Dispatcher dispatcher = (Dispatcher) context.getRequestDispatcher(path);
		state = AsyncState.DISPATCHED;
		dispatcher.request(originalRequest, originalResponse);

		lock.lock();
		try {
			while (true) {
				switch (state) {
					case REDISPATCHING -> state = AsyncState.DISPATCHING;
					case ASYNC_STARTED -> state = AsyncState.ASYNC_WAIT;
					case DISPATCHED, COMPLETING -> state = AsyncState.COMPLETED;
					default -> throw new IllegalStateException("Async state: " + state);
				}

				if (state == AsyncState.DISPATCHING) {
					state = AsyncState.DISPATCHED;
					dispatcher = (Dispatcher) context.getRequestDispatcher(path);
					lock.unlock();
					try {
						dispatcher.async(currentRequest, currentResponse);
					} finally {
						lock.lock();
					}
					continue;
				}

				if (state == AsyncState.ASYNC_WAIT) {
					while (state == AsyncState.ASYNC_WAIT) {
						if (!cond.await(timeout, TimeUnit.MILLISECONDS)) {
							fireOnTimeout();
							return;
						}
					}
					continue;
				}

				assert state == AsyncState.COMPLETED;
				break;
			}
		} catch (final InterruptedException ex) {
			return;
		} finally {
			lock.unlock();
		}
	}

	public void startAsync() {
		lock.lock();
		try {
			if (state != AsyncState.DISPATCHED)
				throw new IllegalStateException("Async state: " + state);
			state = AsyncState.ASYNC_STARTED;
		} finally {
			lock.unlock();
		}
	}

	public void startAsync(final ServletRequest request, final ServletResponse response) {
		lock.lock();
		try {
			if (state != AsyncState.DISPATCHED)
				throw new IllegalStateException("Async state: " + state);
			state = AsyncState.ASYNC_STARTED;
			currentRequest = request;
			currentResponse = response;
		} finally {
			lock.unlock();
		}
	}

	public boolean isAsyncStarted() {
		lock.lock();
		try {
			return state == AsyncState.ASYNC_STARTED || state == AsyncState.ASYNC_WAIT || state == AsyncState.REDISPATCHING || state == AsyncState.COMPLETING;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public ServletRequest getRequest() {
		return currentRequest;
	}

	@Override
	public ServletResponse getResponse() {
		return currentResponse;
	}

	@Override
	public boolean hasOriginalRequestAndResponse() {
		return currentRequest == originalRequest && currentResponse == originalResponse;
	}

	@Override
	public void dispatch() {
		final String path;
		if (currentRequest instanceof HttpServletRequest httpRequest) {
			final String uri = httpRequest.getRequestURI();
			final String contextPath = httpRequest.getContextPath();
			assert uri.startsWith(contextPath);
			path = uri.substring(contextPath.length());
		} else
			path = this.path;
		dispatch(path);
	}

	@Override
	public void dispatch(final String path) {
		dispatch(this.context, path);
	}

	@Override
	public void dispatch(final ServletContext context, final String path) {
		lock.lock();
		try {
			state = switch (state) {
				case ASYNC_STARTED -> AsyncState.REDISPATCHING;
				case ASYNC_WAIT -> AsyncState.DISPATCHING;
				default -> throw new IllegalStateException("Async state " + state);
			};
			this.context = context;
			this.path = path;
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
				case ASYNC_STARTED -> AsyncState.COMPLETING;
				case ASYNC_WAIT -> AsyncState.COMPLETED;
				default -> throw new IllegalStateException("Async state " + state);
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
		listeners.add(listener);
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
		lock.lock();
		try {
			this.timeout = timeout;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public long getTimeout() {
		lock.lock();
		try {
			return timeout;
		} finally {
			lock.unlock();
		}
	}

	private void fireOnTimeout() throws ServletException {
		lock.unlock();
		try {
			final AsyncEvent event = new AsyncEvent(this, currentRequest, currentResponse);
			for (final AsyncListener listener : listeners) {
				try {
					listener.onTimeout(event);
				} catch (final IOException ex) {
					logger.log(Level.SEVERE, "Error in Event handler for event: " + event, ex);
					throw new ServletException("Async timeout", ex);
				}
			}
		} finally {
			lock.lock();
		}
	}
}
