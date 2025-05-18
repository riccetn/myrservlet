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

public final class Dispatcher implements RequestDispatcher {
	private static final Logger logger = Logger.getLogger(Dispatcher.class.getName());

	private final Context context;

	private final Registration registration;

	public Dispatcher(final Context context, final Registration registration) {
		this.context = context;
		this.registration = registration;
	}

	public void request(final Request request, final Response response) throws ServletException, IOException {
		dispatch(request, response);
	}

	@Override
	public void forward(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {
		logger.log(Level.INFO, "FORWARD to servlet ''{0}''", registration.getName());
		dispatch(new ForwardRequest((HttpServletRequest) request), (HttpServletResponse) response);
	}

	@Override
	public void include(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {
		logger.log(Level.INFO, "INCLUDE servlet ''{0}''", registration.getName());
		dispatch(new IncludeRequest((HttpServletRequest) request), (HttpServletResponse) response);
	}

	void dispatch(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		Thread.currentThread().setContextClassLoader(context.getClassLoader());

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
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
			} else {
				response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service Unavailable");
			}
		} catch (final Exception ex) {
			final LogRecord logRecord = new LogRecord(Level.SEVERE, "Exception thrown when handeling request in servlet ''{0}''");
			logRecord.setParameters(new Object[] { registration.getName() });
			logRecord.setThrown(ex);
			logger.log(logRecord);

			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
		} finally {
			Thread.currentThread().setContextClassLoader(null);
		}
	}

}
