package se.narstrom.myr.http;

import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletInputStream;

public interface HttpRequest {
	public Map<String, List<String>> getHeaderfields();

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
