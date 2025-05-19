package se.narstrom.myr.servlet;

import java.io.IOException;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class Async implements AsyncContext {
	private HttpServletRequest request = null;

	private HttpServletResponse response = null;

	private State state = State.DISPATCHING;

	private Context context = null;

	private String path = null;

	synchronized void service(final Request request, final Response response) throws ServletException, IOException, InterruptedException {
		if (state != State.DISPATCHING)
			throw new IllegalStateException();
		this.request = request;
		this.response = response;
		this.context = (Context) request.getServletContext();

		final String uri = request.getRequestURI();
		final String contextPath = context.getContextPath();

		assert uri.startsWith(contextPath);

		this.path = uri.substring(contextPath.length());

		while(state == State.DISPATCHING) {
			this.state = State.DISPATCHED;
			context.getRequestDispatcher(path).dispatch(this.request, this.response);
	
			state = switch(state) {
				case DISPATCHED, COMPLETING -> State.COMPLETED;
				case ASYNC_STARTED -> State.ASYNC_WAIT;
				case REDISPATCHING -> State.DISPATCHING;
				default -> throw new IllegalStateException();
			};

			while(state == State.ASYNC_WAIT)
				wait();
		}

		assert state == State.COMPLETED;
	}

	synchronized void startAsync()  {
		if (state != State.DISPATCHED)
			throw new IllegalStateException();
		this.state = State.ASYNC_STARTED;
	}

	synchronized void startAsync(final ServletRequest request, final ServletResponse response) {
		startAsync();
		this.request = (HttpServletRequest) request;
		this.response = (HttpServletResponse) response;
	}

	synchronized boolean isAsyncStarted() {
		return switch(state) {
			case ASYNC_STARTED, REDISPATCHING, COMPLETING -> true;
			default -> false;
		};
	}

	@Override
	public synchronized ServletRequest getRequest() {
		return request;
	}

	@Override
	public synchronized ServletResponse getResponse() {
		return response;
	}

	@Override
	public synchronized boolean hasOriginalRequestAndResponse() {
		return request instanceof Request && response instanceof Response;
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
	public synchronized void dispatch(final ServletContext context, final String path) {
		state = switch (state) {
			case ASYNC_STARTED -> State.REDISPATCHING;
			case ASYNC_WAIT -> State.DISPATCHING;
			default -> throw new IllegalStateException();
		};
		this.context = (Context) context;
		this.path = path;
		this.notifyAll();
	}

	@Override
	public synchronized void complete() {
		state = switch(state) {
			case ASYNC_STARTED -> State.COMPLETING;
			case ASYNC_WAIT -> State.COMPLETED;
			default -> throw new IllegalStateException();
		};
		this.notifyAll();
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
		} catch(final ReflectiveOperationException ex) {
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
