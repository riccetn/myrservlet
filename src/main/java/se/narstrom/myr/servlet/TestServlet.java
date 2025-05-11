package se.narstrom.myr.servlet;

import java.io.IOException;
import java.time.Duration;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class TestServlet extends HttpServlet {
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/plain");
		final StringBuilder sb = new StringBuilder();
		for (final Cookie cookie : request.getCookies()) {
			sb.append(cookie).append("\n");
		}
		
		final Cookie cookie = new Cookie("abc", "def");
		cookie.setMaxAge((int) Duration.ofDays(30).toSeconds());
		response.addCookie(cookie);
		response.setContentLength(sb.length());
		response.getWriter().write(sb.toString());
	}
}
