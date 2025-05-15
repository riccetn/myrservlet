package se.narstrom.myr.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

public final class Container implements AutoCloseable {
	private final Logger logger = Logger.getLogger(getClass().getName());

	private final Context rootContext;

	private final Map<String, Context> contexes = new HashMap<>();

	public Container(final Context rootContext) {
		this.rootContext = rootContext;
	}

	public void init() throws ServletException {
		logger.info("Initializing container");
		rootContext.init();
	}

	@Override
	public void close() {
		rootContext.close();
	}

	public void service(final Request request, final Response response) throws IOException, ServletException {
		final String uri = request.getRequestURI();
		if (uri.charAt(0) != '/') {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
			return;
		}

		int slash = uri.indexOf('/', 1);
		if(slash == -1)
			slash = uri.length();

		final String contextUri = uri.substring(1, slash);

		Context context = null;

		if(!contextUri.isEmpty())
			context = contexes.get(contextUri);

		if(context == null)
			context = rootContext;

		request.setContext(context);
		context.service(request, response);
	}

	public void addContext(final String url, final Context context) {
		Objects.requireNonNull(url);
		Objects.requireNonNull(context);

		if (url.contains("/"))
			throw new IllegalArgumentException();

		if (url.isEmpty() || url.equals(".") || url.equals(".."))
			throw new IllegalArgumentException();

		contexes.put(url, context);
	}
}
