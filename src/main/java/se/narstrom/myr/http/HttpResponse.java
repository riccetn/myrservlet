package se.narstrom.myr.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

public interface HttpResponse {
	public void addHeader(final String nameString, final String valueString);
	public OutputStream getOutputStream() throws IOException;
	public int getStatus();
	public boolean isCommitted();
	public void reset();
	public void setHeader(final String nameString, final String valueString);
	public void setStatus(int status);
	public boolean containsHeader(final String name);
	public String getHeader(final String name);
	public Collection<String> getHeaders(final String name);
	public Collection<String> getHeaderNames();
}
