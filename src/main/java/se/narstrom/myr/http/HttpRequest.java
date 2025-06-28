package se.narstrom.myr.http;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;

import jakarta.servlet.ServletConnection;

public interface HttpRequest {
	public String getHeader(final String name);
	public Enumeration<String> getHeaderNames();
	public Enumeration<String> getHeaders(final String name);
	public InputStream getInputStream();
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
	public int getServerPort();
	public ServletConnection getServletConnection();
	public boolean isSecure();
	public boolean isTrailerFieldsReady();
	public Map<String, String> getTrailerFields();
}
