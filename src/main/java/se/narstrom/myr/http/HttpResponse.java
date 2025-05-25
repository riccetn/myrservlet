package se.narstrom.myr.http;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletOutputStream;

public interface HttpResponse {
	void addHeader(final String name, final String value);

	void close() throws IOException;

	void commit() throws IOException;

	Map<String, List<String>> getHeaders();

	ServletOutputStream getOutputStream() throws IOException;

	int getStatus();

	boolean isClosed();

	boolean isCommitted();

	void reset();

	void setHeader(final String name, final String value);

	void setStatus(final int status);
}
