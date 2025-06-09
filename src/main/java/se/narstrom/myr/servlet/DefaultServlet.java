package se.narstrom.myr.servlet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serial;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class DefaultServlet extends HttpServlet {
	@Serial
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		throw new FileNotFoundException();
	}
}
