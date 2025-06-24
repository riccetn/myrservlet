package se.narstrom.myr.servlet;

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
import se.narstrom.myr.http.HttpRequest;
import se.narstrom.myr.http.HttpResponse;
import se.narstrom.myr.servlet.context.Context;
import se.narstrom.myr.servlet.request.Request;
import se.narstrom.myr.servlet.response.Response;

public final class Dispatcher implements RequestDispatcher {
	private static final Logger logger = Logger.getLogger(Dispatcher.class.getName());

	private final Context context;

	private final Mapping mapping;

	private final Registration registration;

	private HttpServletResponse response = null;;

	public Dispatcher(final Context context, final Mapping mapping, final Registration registration) {
		this.context = context;
		this.mapping = mapping;
		this.registration = registration;
	}

	Registration getRegistration() {
		return registration;
	}

	public Context getContext() {
		return context;
	}

	public Mapping getMapping() {
		return mapping;
	}

	public HttpServletResponse getResponse() {
		return this.response;
	}

	public void request(final HttpRequest request, final HttpResponse response) throws ServletException, IOException {
		logger.log(Level.INFO, "DISPATCH for servlet ''{0}, asyncSupported: {1}", new Object[] { registration.getName(), registration.isAsyncSupported() });
		final Request wrappedRequest = new Request(request, this);
		final Response wrappedResponse = new Response(response, context);
		dispatch(wrappedRequest, wrappedResponse);
		wrappedResponse.close();
	}

	@Override
	public void forward(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {
		logger.log(Level.INFO, "FORWARD to servlet ''{0}''", registration.getName());
		response.reset();
		dispatch(new ForwardRequest((HttpServletRequest) request, this), (HttpServletResponse) response);
	}

	@Override
	public void include(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {
		logger.log(Level.INFO, "INCLUDE servlet ''{0}''", registration.getName());
		dispatch(new IncludeRequest((HttpServletRequest) request), new IncludeResponse((HttpServletResponse) response));
	}

	public void error(final HttpRequest request, final HttpResponse response, final Throwable throwable, final int errorCode) throws ServletException, IOException {
		final ErrorRequest errorRequest = new ErrorRequest(request, this);
		errorRequest.setAttribute(ERROR_EXCEPTION, throwable);
		errorRequest.setAttribute(ERROR_EXCEPTION_TYPE, throwable.getClass());
		errorRequest.setAttribute(ERROR_MESSAGE, throwable.getMessage());
		errorRequest.setAttribute(ERROR_METHOD, request.getMethod());
		errorRequest.setAttribute(ERROR_QUERY_STRING, request.getQueryString());
		errorRequest.setAttribute(ERROR_REQUEST_URI, request.getRequestURI());
		errorRequest.setAttribute(ERROR_STATUS_CODE, errorCode);
		errorRequest.setAttribute(ERROR_SERVLET_NAME, "TODO: Get servlet name");

		final Response errorResponse = new Response(response, context);
		errorResponse.reset();
		errorResponse.setStatus(errorCode);

		dispatch(errorRequest, errorResponse);
		errorResponse.close();
	}

	private void dispatch(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		Thread.currentThread().setContextClassLoader(context.getClassLoader());

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
