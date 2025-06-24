package se.narstrom.myr.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import se.narstrom.myr.http.HttpRequest;
import se.narstrom.myr.http.HttpResponse;
import se.narstrom.myr.http.exceptions.BadRequest;
import se.narstrom.myr.http.exceptions.HttpStatusCodeException;
import se.narstrom.myr.http.exceptions.NotFound;
import se.narstrom.myr.servlet.context.Context;

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

	public void service(final HttpRequest request, final HttpResponse response) throws IOException, HttpStatusCodeException {
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

		logger.log(Level.INFO, "Dispatching: {0} to context {1}", new Object[] { uri, context.getServletContextName() });

		context.service(request, response);
	}

	public void addContext(final Context context) {
		Objects.requireNonNull(context);
		contexes.put(context.getContextPath(), context);
	}

	public Context removeContext(final String contextPath) {
		return contexes.remove(contextPath);
	}
}
