package se.narstrom.myr.http;

import java.util.Enumeration;

import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletInputStream;

public interface HttpRequest {
	public String getHeader(final String name);

	public Enumeration<String> getHeaderNames();

	public Enumeration<String> getHeaders(final String name);

	public ServletInputStream getInputStream();

	public String getLocalAddr();

	public String getLocalName();

	public int getLocalPort();

	public String getMethod();

	public String getProtocol();

	public String getProtocolRequestId();

	public String getQueryString();

	public String getRemoteAddr();

	public String getRemoteHost();

	public int getRemotePort();

	public String getRequestURI();

	public String getScheme();

	public String getServerName();

	public ServletConnection getServletConnection();

	public boolean isSecure();
}
