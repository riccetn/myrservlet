package se.narstrom.myr.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

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

	public void service(final Request request, final Response response) throws IOException, ServletException, InterruptedException {
		final String uri = request.getRequestURI();
		if (uri.charAt(0) != '/') {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
			return;
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
			logger.log(Level.WARNING, "No context foubnd for URI: {0}", uri);
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
			return;
		}

		logger.log(Level.INFO, "Dispatching: {0} to context {1}", new Object[] { uri, context.getServletContextName() });

		final String contextRelativePath = uri.substring(contextUri.length());
		request.setContext(context);
		response.setContext(context);
		request.getAsyncContext().service(request, response);
	}

	public void addContext(final Context context) {
		Objects.requireNonNull(context);
		contexes.put(context.getContextPath(), context);
	}

	public Context removeContext(final String contextPath) {
		return contexes.remove(contextPath);
	}
}
