package se.narstrom.myr.http.v1;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import se.narstrom.myr.http.HttpResponse;
import se.narstrom.myr.http.semantics.FieldValue;
import se.narstrom.myr.http.semantics.Token;

public final class Http1Response implements HttpResponse {
	private final HashMap<Token, ArrayList<FieldValue>> headerFields = new HashMap<>();

	private final OutputStream socketOutputStream;

	private State state = State.OPEN;

	private int status = HttpServletResponse.SC_OK;

	private ServletOutputStream outputStream = null;

	public Http1Response(final OutputStream socketOutputStream) {
		this.socketOutputStream = socketOutputStream;
	}

	@Override
	public void addHeader(final String nameString, final String valueString) {
		if (isCommitted())
			return;
		if (nameString == null || valueString == null)
			return;

		final Token name = new Token(nameString);
		final FieldValue value = new FieldValue(valueString);

		headerFields.computeIfAbsent(name, _ -> new ArrayList<>()).add(value);
	}

	@Override
	public void close() throws IOException {
		commit();
		state = State.CLOSED;
	}

	@Override
	public void commit() throws IOException {
		if (state != State.OPEN)
			return;
		state = State.COMMITED;

		socketOutputStream.write(("HTTP/1.1 " + status + "\r\n").getBytes());
		for (final Map.Entry<Token, ArrayList<FieldValue>> entry : headerFields.entrySet()) {
			final Token name = entry.getKey();
			final List<FieldValue> values = entry.getValue();
			for (final FieldValue value : values)
				socketOutputStream.write((name.toString() + ": " + value.toString() + "\r\n").getBytes());
		}
		socketOutputStream.write("\r\n".getBytes());
	}

	@Override
	public Map<String, List<String>> getHeaders() {
		return headerFields.entrySet().stream().collect(Collectors.toUnmodifiableMap(entry -> entry.getKey().toString(), entry -> entry.getValue().stream().map(FieldValue::toString).toList()));
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		final List<FieldValue> contentLength = headerFields.get(new Token("content-length"));
		if (contentLength != null) {
			long length = -1L;
			try {
				length = Long.parseLong(contentLength.getFirst().toString());
			} catch (final NumberFormatException _) {
			}
			if (length >= 0) {
				headerFields.remove(new Token("transfer-encoding"));
				outputStream = new LengthOutputStream(socketOutputStream, length);
			}
		}

		if (outputStream == null) {
			headerFields.put(new Token("transfer-encoding"), new ArrayList<>(List.of(new FieldValue("chunked"))));
			headerFields.remove(new Token("content-length"));
			outputStream = new ChunkedOutputStream(socketOutputStream);
		}

		commit();

		return outputStream;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public boolean isClosed() {
		return state == State.CLOSED;
	}

	@Override
	public boolean isCommitted() {
		return state == State.COMMITED || state == State.CLOSED;
	}

	@Override
	public void reset() {
		if (state != State.OPEN)
			throw new IllegalStateException("Response is already committed");
		status = HttpServletResponse.SC_OK;
		headerFields.clear();
	}

	@Override
	public void setHeader(final String nameString, final String valueString) {
		final Token name = new Token(nameString.toLowerCase());
		if (valueString != null) {
			final FieldValue value = new FieldValue(valueString);
			headerFields.put(name, new ArrayList<>(List.of(value)));
		} else {
			headerFields.remove(name);
		}
	}

	@Override
	public void setStatus(int status) {
		if (isCommitted())
			return;
		this.status = status;
	}

	private enum State {
		OPEN, COMMITED, CLOSED
	}
}
