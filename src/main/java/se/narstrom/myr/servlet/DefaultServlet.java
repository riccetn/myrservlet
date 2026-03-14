package se.narstrom.myr.servlet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class DefaultServlet extends HttpServlet {
	@Serial
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		final String pathTranslated = request.getPathTranslated();
		if (pathTranslated == null)
			throw new FileNotFoundException();

		final Path path = Path.of(pathTranslated);

		if (!Files.exists(path))
			throw new FileNotFoundException(pathTranslated);

		if (Files.isDirectory(path) && !request.getPathInfo().endsWith("/")) {
			response.sendRedirect(request.getRequestURI() + "/");
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
}
