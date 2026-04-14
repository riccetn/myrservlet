package se.narstrom.myr.servlet.dispatcher;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import se.narstrom.myr.servlet.Mapping;
import se.narstrom.myr.servlet.async.AsyncHttpRequest;
import se.narstrom.myr.servlet.async.AsyncRequest;
import se.narstrom.myr.servlet.context.Context;
import se.narstrom.myr.servlet.request.Request;
import se.narstrom.myr.servlet.response.Response;
import se.narstrom.myr.servlet.servlet.MyrServletRegistration;
import se.narstrom.myr.web.url.Query;

public final class Dispatcher implements RequestDispatcher {
	private static final Logger logger = Logger.getLogger(Dispatcher.class.getName());

	private final Context context;

	private final Mapping mapping;

	private final MyrServletRegistration servletRegistration;

	private final Query query;

	private ServletRequest request = null;

	private ServletResponse response = null;;

	public Dispatcher(final Context context, final Mapping mapping, final MyrServletRegistration servlet, final Query query) {
		this.context = context;
		this.mapping = mapping;
		this.servletRegistration = servlet;
		this.query = query;
	}

	public Dispatcher(final Context context, final MyrServletRegistration servlet) {
		this.context = context;
		this.mapping = null;
		this.servletRegistration = servlet;
		this.query = null;
	}

	public MyrServletRegistration getRegistration() {
		return servletRegistration;
	}

	public Context getContext() {
		return context;
	}

	public Mapping getMapping() {
		return mapping;
	}

	public ServletRequest getRequest() {
		return this.request;
	}

	public ServletResponse getResponse() {
		return this.response;
	}

	public Query getQuery() {
		return this.query;
	}

	public boolean isAsyncSupported() {
		return getRegistration().isAsyncSupported();
	}

	public Request getOriginalRequest() {
		ServletRequest request = this.request;
		while (request instanceof ServletRequestWrapper wrapper)
			request = wrapper.getRequest();
		return (Request) request;
	}

	public void request(final Request request, final Response response) throws ServletException, IOException {
		logger.log(Level.INFO, "DISPATCH for servlet ''{0}'', asyncSupported: {1}", new Object[] { getRegistration().getName(), getRegistration().isAsyncSupported() });
		request.setDispatcher(this);
		response.setDispatcher(this);
		dispatch(request, response, DispatcherType.REQUEST);
	}

	public void async(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {
		logger.log(Level.INFO, "ASYNC for servlet ''{0}'', asyncSupported: {1}", new Object[] { getRegistration().getName(), getRegistration().isAsyncSupported() });
		final ServletRequest asyncRequest;
		if (request instanceof HttpServletRequest httpRequest)
			asyncRequest = new AsyncHttpRequest(httpRequest, this);
		else
			asyncRequest = new AsyncRequest(request, this);
		dispatch(asyncRequest, response, DispatcherType.ASYNC);
	}

	@Override
	public void forward(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {
		logger.log(Level.INFO, "FORWARD to servlet ''{0}''", getRegistration().getName());
		response.reset();
		dispatch(new ForwardRequest((HttpServletRequest) request, this), (HttpServletResponse) response, DispatcherType.FORWARD);
	}

	@Override
	public void include(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {
		logger.log(Level.INFO, "INCLUDE to servlet ''{0}''", getRegistration().getName());
		dispatch(new IncludeRequest((HttpServletRequest) request), new IncludeResponse((HttpServletResponse) response), DispatcherType.INCLUDE);
	}

	public void error(final Request request, final Response response, final Throwable throwable, final int errorCode, final String message) throws ServletException, IOException {
		logger.log(Level.INFO, "ERROR to servlet ''{0}''", getRegistration().getName());
		final ErrorRequest errorRequest = new ErrorRequest(request, this);
		if (throwable != null) {
			errorRequest.setAttribute(ERROR_EXCEPTION, throwable);
			errorRequest.setAttribute(ERROR_EXCEPTION_TYPE, throwable.getClass());
			errorRequest.setAttribute(ERROR_MESSAGE, throwable.getMessage());
		}
		if (message != null) {
			errorRequest.setAttribute(ERROR_MESSAGE, message);
		}
		errorRequest.setAttribute(ERROR_METHOD, request.getMethod());
		errorRequest.setAttribute(ERROR_QUERY_STRING, request.getQueryString());
		errorRequest.setAttribute(ERROR_REQUEST_URI, request.getRequestURI());
		errorRequest.setAttribute(ERROR_STATUS_CODE, errorCode);
		errorRequest.setAttribute(ERROR_SERVLET_NAME, request.getHttpServletMapping().getServletName());

		response.reset();
		response.setStatus(errorCode);

		dispatch(errorRequest, response, DispatcherType.ERROR);
	}

	private void dispatch(final ServletRequest request, final ServletResponse response, final DispatcherType dispatcherType) throws ServletException, IOException {
		this.request = request;
		this.response = response;
		if (mapping != null)
			context.service(request, response, mapping, dispatcherType);
		else
			context.service(request, response, servletRegistration, dispatcherType);
	}
}
