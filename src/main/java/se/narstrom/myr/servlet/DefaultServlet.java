package se.narstrom.myr.servlet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class DefaultServlet extends HttpServlet {
	@Serial
	private static final long serialVersionUID = 1L;

	private List<String> welcomeFiles = List.of();

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		final String pathTranslated = request.getPathTranslated();
		if (pathTranslated == null) {
			throw new FileNotFoundException(request.getPathInfo());
		}

		final Path path = Path.of(pathTranslated);

		if (!Files.exists(path))
			throw new FileNotFoundException(pathTranslated);

		if (Files.isDirectory(path)) {
			handleDirectory(request, response, path);
			return;
		}

		final String mimeType = request.getServletContext().getMimeType(request.getPathInfo());
		if (mimeType != null) {
			response.setContentType(mimeType);
		}

		if (request.getMethod().equals("HEAD"))
			return;

		Files.copy(path, response.getOutputStream());
	}

	public void setWelcomeFiles(final List<String> welcomeFiles) {
		this.welcomeFiles = welcomeFiles;
	}

	private void handleDirectory(final HttpServletRequest request, final HttpServletResponse response, final Path directory) throws ServletException, IOException {
		if (!request.getPathInfo().endsWith("/")) {
			response.sendRedirect(request.getRequestURI() + "/");
			return;
		}

		for (final String welcomeFile : welcomeFiles) {
			if (Files.exists(directory.resolve(welcomeFile))) {
				request.getRequestDispatcher(request.getServletPath() + request.getPathInfo() + welcomeFile).forward(request, response);
				return;
			}
		}

		response.sendError(HttpServletResponse.SC_FORBIDDEN, "Directory Listing not implemented");
	}
}
