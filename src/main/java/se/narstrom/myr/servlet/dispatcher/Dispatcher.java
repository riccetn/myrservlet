package se.narstrom.myr.servlet.dispatcher;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import se.narstrom.myr.servlet.Mapping;
import se.narstrom.myr.servlet.Registration;
import se.narstrom.myr.servlet.async.AsyncRequest;
import se.narstrom.myr.servlet.context.Context;
import se.narstrom.myr.servlet.request.Request;
import se.narstrom.myr.servlet.response.Response;
import se.narstrom.myr.uri.Query;

public final class Dispatcher implements RequestDispatcher {
	private static final Logger logger = Logger.getLogger(Dispatcher.class.getName());

	private final Context context;

	private final Mapping mapping;

	private final Registration registration;

	private final Query query;

	private HttpServletRequest request = null;

	private HttpServletResponse response = null;;

	public Dispatcher(final Context context, final Mapping mapping, final Registration registration, final Query query) {
		this.context = context;
		this.mapping = mapping;
		this.registration = registration;
		this.query = query;
	}

	public Registration getRegistration() {
		return registration;
	}

	public Context getContext() {
		return context;
	}

	public Mapping getMapping() {
		return mapping;
	}

	public HttpServletRequest getRequest() {
		return this.request;
	}

	public HttpServletResponse getResponse() {
		return this.response;
	}

	public Query getQuery() {
		return this.query;
	}

	public void request(final Request request, final Response response) throws ServletException, IOException {
		logger.log(Level.INFO, "DISPATCH for servlet ''{0}'', asyncSupported: {1}", new Object[] { registration.getName(), registration.isAsyncSupported() });
		request.setDispatcher(this);
		response.setDispatcher(this);
		dispatch(request, response);
	}

	public void async(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {
		logger.log(Level.INFO, "ASYNC for servlet ''{0}'', asyncSupported: {1}", new Object[] { registration.getName(), registration.isAsyncSupported() });
		dispatch(new AsyncRequest((HttpServletRequest) request, this), (HttpServletResponse) response);
	}

	@Override
	public void forward(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {
		logger.log(Level.INFO, "FORWARD to servlet ''{0}''", registration.getName());
		response.reset();
		dispatch(new ForwardRequest((HttpServletRequest) request, this), (HttpServletResponse) response);
	}

	@Override
	public void include(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {
		logger.log(Level.INFO, "INCLUDE to servlet ''{0}''", registration.getName());
		dispatch(new IncludeRequest((HttpServletRequest) request), new IncludeResponse((HttpServletResponse) response));
	}

	public void error(final Request request, final Response response, final Throwable throwable, final int errorCode) throws ServletException, IOException {
		final ErrorRequest errorRequest = new ErrorRequest(request);
		errorRequest.setAttribute(ERROR_EXCEPTION, throwable);
		errorRequest.setAttribute(ERROR_EXCEPTION_TYPE, throwable.getClass());
		errorRequest.setAttribute(ERROR_MESSAGE, throwable.getMessage());
		errorRequest.setAttribute(ERROR_METHOD, request.getMethod());
		errorRequest.setAttribute(ERROR_QUERY_STRING, request.getQueryString());
		errorRequest.setAttribute(ERROR_REQUEST_URI, request.getRequestURI());
		errorRequest.setAttribute(ERROR_STATUS_CODE, errorCode);
		errorRequest.setAttribute(ERROR_SERVLET_NAME, "TODO: Get servlet name");

		response.reset();
		response.setStatus(errorCode);

		dispatch(errorRequest, response);
	}

	private void dispatch(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		Thread.currentThread().setContextClassLoader(context.getClassLoader());

		this.request = request;
		this.response = response;

		try {
			registration.init();
			registration.getServlet().service(request, response);
		} catch (final UnavailableException ex) {
			final LogRecord logRecord = new LogRecord(Level.WARNING, "Servlet ''{0}'' {1} unavailable");
			logRecord.setParameters(new Object[] { registration.getName(), ex.isPermanent() ? "permanently" : "temporary" });
			logRecord.setThrown(ex);
			logger.log(logRecord);

			if (ex.isPermanent()) {
				registration.destroy();
			}

			throw ex;
		} catch (final ServletException | IOException ex) {
			final LogRecord logRecord = new LogRecord(Level.WARNING, "Exception thrown when handeling request in servlet ''{0}''");
			logRecord.setParameters(new Object[] { registration.getName() });
			logRecord.setThrown(ex);
			logger.log(logRecord);
			throw ex;
		} catch (final Exception ex) {
			final LogRecord logRecord = new LogRecord(Level.WARNING, "Exception thrown when handeling request in servlet ''{0}''");
			logRecord.setParameters(new Object[] { registration.getName() });
			logRecord.setThrown(ex);
			logger.log(logRecord);
			throw new ServletException(ex);
		} finally {
			Thread.currentThread().setContextClassLoader(null);
		}
	}

}
