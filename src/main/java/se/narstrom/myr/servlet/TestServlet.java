package se.narstrom.myr.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public final class TestServlet extends HttpServlet {
	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/plain");
		final PrintWriter writer = response.getWriter();
		for (final Map.Entry<String, String[]> param : request.getParameterMap().entrySet()) {
			writer.printf("%s: %s%n", param.getKey(), String.join(",", param.getValue()));
		}
	}
}
