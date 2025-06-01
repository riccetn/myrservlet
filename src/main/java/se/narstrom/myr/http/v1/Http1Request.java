package se.narstrom.myr.http.v1;

import java.net.Socket;
import java.util.Enumeration;
import java.util.Objects;

import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletInputStream;
import se.narstrom.myr.http.semantics.Fields;
import se.narstrom.myr.http.semantics.Method;
import se.narstrom.myr.servlet.StubRequest;
import se.narstrom.myr.uri.Query;

public final class Http1Request extends StubRequest {
	private final Method method;

	private final AbsolutePath requestUri;

	private final Query query;

	private final Fields headerFields;

	private final Socket socket;

	private final ServletInputStream inputStream;

	public Http1Request(final Method method, final AbsolutePath requestUri, final Query query, final Fields headerFields, final Socket socket, final ServletInputStream in) {
		this.method = method;
		this.requestUri = requestUri;
		this.query = query;
		this.headerFields = headerFields;
		this.socket = socket;
		this.inputStream = in;
	}

	@Override
	public String getHeader(final String name) {
		Objects.requireNonNull(name);
		return headerFields.getField(name);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return headerFields.getFieldNames();
	}

	@Override
	public Enumeration<String> getHeaders(final String name) {
		Objects.requireNonNull(name);
		return headerFields.getFields(name);
	}

	@Override
	public ServletInputStream getInputStream() {
		return inputStream;
	}

	@Override
	public String getLocalAddr() {
		return socket.getLocalAddress().getHostAddress();
	}

	@Override
	public String getLocalName() {
		return socket.getLocalAddress().getHostName();
	}

	@Override
	public int getLocalPort() {
		return socket.getLocalPort();
	}

	@Override
	public String getMethod() {
		return method.toString();
	}

	@Override
	public String getProtocol() {
		return "HTTP/1.1";
	}

	@Override
	public String getProtocolRequestId() {
		return "";
	}

	@Override
	public String getQueryString() {
		return query.toString();
	}

	@Override
	public String getRemoteAddr() {
		return socket.getInetAddress().getHostAddress();
	}

	@Override
	public String getRemoteHost() {
		return socket.getInetAddress().getHostName();
	}

	@Override
	public int getRemotePort() {
		return socket.getPort();
	}

	@Override
	public String getRequestURI() {
		return requestUri.toString();
	}

	@Override
	public String getScheme() {
		return "http";
	}

	@Override
	public String getServerName() {
		final String host = getHeader("host");
		final int colon = host.indexOf(':');
		if (colon == -1)
			return host;
		else
			return host.substring(0, colon);
	}

	@Override
	public int getServerPort() {
		final String host = getHeader("host");
		final int colon = host.indexOf(':');
		if (colon == -1)
			return getLocalPort();
		else
			return Integer.parseInt(host.substring(colon + 1));
	}

	@Override
	public ServletConnection getServletConnection() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSecure() {
		return false;
	}
}
