package se.narstrom.myr.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;
import java.util.logging.Logger;

public final class Container implements AutoCloseable {
	private final Logger logger = Logger.getLogger(getClass().getName());

	private final Context rootContext;

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

	public void service(final ServletRequest request, final ServletResponse response) throws IOException, ServletException {
		rootContext.service(request, response);
	}
}
