package se.narstrom.myr.http.v1;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletResponse;
import se.narstrom.myr.MappingCollection;
import se.narstrom.myr.http.HttpResponse;
import se.narstrom.myr.http.semantics.FieldName;
import se.narstrom.myr.http.semantics.FieldValue;
import se.narstrom.myr.http.semantics.Token;

public final class Http1Response implements HttpResponse {
	private final HashMap<FieldName, ArrayList<FieldValue>> headerFields = new HashMap<>();

	private final OutputStream socketOutputStream;

	private State state = State.OPEN;

	private int status = HttpServletResponse.SC_OK;

	private OutputStream outputStream = null;

	public Http1Response(final OutputStream socketOutputStream) {
		this.socketOutputStream = socketOutputStream;
	}

	@Override
	public void addHeader(final String nameString, final String valueString) {
		if (isCommitted())
			return;
		if (nameString == null || valueString == null)
			return;

		final FieldName name = new FieldName(false, new Token(nameString));
		final FieldValue value = new FieldValue(valueString);

		headerFields.computeIfAbsent(name, _ -> new ArrayList<>()).add(value);
	}

	public void close() throws IOException {
		if (outputStream != null)
			outputStream.close();
		commit();
		state = State.CLOSED;
	}

	public void commit() throws IOException {
		if (state != State.OPEN)
			return;
		state = State.COMMITED;

		socketOutputStream.write(("HTTP/1.1 " + status + "\r\n").getBytes());
		for (final Map.Entry<FieldName, ArrayList<FieldValue>> entry : headerFields.entrySet()) {
			final FieldName name = entry.getKey();
			final List<FieldValue> values = entry.getValue();
			for (final FieldValue value : values)
				socketOutputStream.write((name.toString() + ": " + value.toString() + "\r\n").getBytes());
		}
		socketOutputStream.write("\r\n".getBytes());
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		final List<FieldValue> contentLength = headerFields.get(new FieldName("content-length"));
		if (contentLength != null) {
			long length = -1L;
			try {
				length = Long.parseLong(contentLength.getFirst().toString());
			} catch (final NumberFormatException _) {
			}
			if (length >= 0) {
				headerFields.remove(new FieldName("transfer-encoding"));
				outputStream = new LengthOutputStream(socketOutputStream, length);
			}
		}

		if (outputStream == null) {
			headerFields.put(new FieldName("transfer-encoding"), new ArrayList<>(List.of(new FieldValue("chunked"))));
			headerFields.remove(new FieldName("content-length"));
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
		final FieldName name = new FieldName(false, new Token(nameString));
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

	@Override
	public boolean containsHeader(final String name) {
		Objects.requireNonNull(name);
		return headerFields.containsKey(new FieldName(name));
	}

	@Override
	public String getHeader(final String name) {
		Objects.requireNonNull(name);
		final List<FieldValue> values = headerFields.get(new FieldName(name));
		if (values == null)
			return null;
		return values.getFirst().toString();
	}

	@Override
	public Collection<String> getHeaders(final String name) {
		Objects.requireNonNull(name);
		final List<FieldValue> values = headerFields.get(new FieldName(name));
		if (values == null)
			return null;
		return new MappingCollection<>(values, FieldValue::toString);
	}

	@Override
	public Collection<String> getHeaderNames() {
		return new MappingCollection<>(headerFields.keySet(), FieldName::toString);
	}

	private enum State {
		OPEN, COMMITED, CLOSED
	}
}
