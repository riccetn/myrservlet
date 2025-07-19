package se.narstrom.myr.servlet.container;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServletResponse;
import se.narstrom.myr.http.HttpRequest;
import se.narstrom.myr.http.HttpResponse;
import se.narstrom.myr.http.exceptions.BadRequest;
import se.narstrom.myr.http.exceptions.HttpStatusCodeException;
import se.narstrom.myr.http.exceptions.NotFound;
import se.narstrom.myr.servlet.async.AsyncHandler;
import se.narstrom.myr.servlet.context.Context;
import se.narstrom.myr.servlet.request.Request;
import se.narstrom.myr.servlet.response.Response;

public final class Container implements AutoCloseable {
	private final Logger logger = Logger.getLogger(getClass().getName());

	private final Map<String, Context> contexes = new HashMap<>();

	public Container() {
	}

	public void init() throws ServletException {
		logger.info("Initializing container");
	}

	@Override
	public void close() {
	}

	public void service(final HttpRequest httpRequest, final HttpResponse httpResponse) throws IOException, HttpStatusCodeException {
		final Request request = new Request(httpRequest);
		final Response response = new Response(httpResponse);

		final String uri = request.getRequestURI();
		if (uri.charAt(0) != '/') {
			throw new BadRequest("Invalid URI");
		}

		int slash = uri.indexOf('/', 1);
		if (slash == -1)
			slash = uri.length();

		final String contextUri = uri.substring(0, slash);

		Context context = null;

		if (!contextUri.isEmpty())
			context = contexes.get(contextUri);

		if (context == null)
			context = contexes.get("");

		if (context == null) {
			logger.log(Level.WARNING, "No context found for URI: {0}", uri);
			throw new NotFound("No context");
		}

		final String path = uri.substring(contextUri.length());

		logger.log(Level.INFO, "Dispatching: {0} to context {1}", new Object[] { uri, context.getServletContextName() });

		final AsyncHandler async = new AsyncHandler(context, path, request, response);
		try {
			async.service();
		} catch (final ServletException | IOException ex) {
			final LogRecord logRecord = new LogRecord(Level.WARNING, "Exception from dispatch in context ''{0}''");
			logRecord.setParameters(new Object[] { context.getServletContextName() });
			logRecord.setThrown(ex);
			logger.log(logRecord);

			if (!response.isCommitted())
				handleException(context, request, response, ex);
		}
		response.close();
	}

	private void handleException(final Context context, final Request request, final Response response, final Throwable ex) {
		try {
			final String path = context.getExceptionMapping(ex);

			if (path != null) {
				context.getRequestDispatcher(path).error(request, response, ex, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}

			if (ex instanceof ServletException sex) {
				final Throwable cause = sex.getRootCause();
				if (cause != null)
					handleException(context, request, response, sex.getRootCause());
			}

			switch (ex) {
				case UnavailableException uex when !uex.isPermanent() -> handleError(context, request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, ex);
				case UnavailableException _ -> handleError(context, request, response, HttpServletResponse.SC_NOT_FOUND, ex);
				case FileNotFoundException _ -> handleError(context, request, response, HttpServletResponse.SC_NOT_FOUND, ex);
				default -> handleError(context, request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
			}
		} catch (final ServletException | IOException ex2) {
			logger.log(Level.SEVERE, "Error in error-page dispatch", ex2);
		}
	}

	private void handleError(final Context context, final Request request, final Response response, final int status, final Throwable ex) {
		try {
			final String path = context.getErrorMapping(status);

			if (path == null) {
				String message = ex.getMessage();
				if (message == null)
					message = "Unknown Error";
				response.reset();
				response.setStatus(status);
				response.setContentType("text/plain");
				response.setContentLength(message.length());
				response.getWriter().write(message);
				response.flushBuffer();
				return;
			}

			context.getRequestDispatcher(path).error(request, response, ex, status);
		} catch (final ServletException | IOException ex2) {
			logger.log(Level.SEVERE, "Error in error-page dispatch", ex2);
		}
	}

	public void addContext(final Context context) {
		Objects.requireNonNull(context);
		contexes.put(context.getContextPath(), context);
	}

	public Context removeContext(final String contextPath) {
		return contexes.remove(contextPath);
	}
}
