package se.narstrom.myr.http.v1;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletInputStream;
import se.narstrom.myr.http.HttpRequest;
import se.narstrom.myr.http.semantics.FieldValue;
import se.narstrom.myr.http.semantics.Method;
import se.narstrom.myr.http.semantics.Token;
import se.narstrom.myr.uri.Query;

public class Http1Request implements HttpRequest {
	private final Method method;

	private final AbsolutePath requestUri;

	private final Query query;

	private final Map<Token, List<FieldValue>> headerFields;

	private final Socket socket;

	private final ServletInputStream inputStream;

	public Http1Request(final Method method, final AbsolutePath requestUri, final Query query, final Map<Token, List<FieldValue>> headerFields, final Socket socket, final ServletInputStream in) {
		this.method = method;
		this.requestUri = requestUri;
		this.query = query;
		this.headerFields = headerFields;
		this.socket = socket;
		this.inputStream = in;
	}

	@Override
	public Map<String, List<String>> getHeaderfields() {
		return headerFields.entrySet().stream().collect(Collectors.toUnmodifiableMap(entry -> entry.getKey().toString(), entry -> entry.getValue().stream().map(FieldValue::toString).toList()));
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
		final String host = headerFields.get(new Token("host")).getFirst().toString();
		final int colon = host.indexOf(':');
		if (colon == -1)
			return host;
		else
			return host.substring(0, colon);
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
