package se.narstrom.myr.test;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class GoodByeServlet extends HttpServlet {
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/plain");
		final String message = "Good Bye World!";
		response.setContentLength(message.length());
		response.getWriter().write(message.toString());
	}
}
