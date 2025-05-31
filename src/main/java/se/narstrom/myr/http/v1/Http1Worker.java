package se.narstrom.myr.http.v1;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletInputStream;
import se.narstrom.myr.http.semantics.Field;
import se.narstrom.myr.http.semantics.Fields;
import se.narstrom.myr.net.ServerClientWorker;
import se.narstrom.myr.servlet.Container;

public final class Http1Worker implements ServerClientWorker {
	private final Container container;

	private final Socket socket;

	private final InputStream in;

	private final OutputStream out;

	public Http1Worker(final Container container, final Socket socket) throws IOException {
		this.container = container;
		this.socket = socket;
		this.in = new BufferedInputStream(socket.getInputStream());
		this.out = socket.getOutputStream();
	}

	@Override
	public void run() throws IOException {
		final RequestLine requestLine = RequestLine.parse(readLine());
		final Fields headerFields = readFields();

		final ServletInputStream servletInputStream = createServletStream(headerFields);

		final Http1Request request = new Http1Request(requestLine.method(), requestLine.target().absolutePath(), requestLine.target().query(), headerFields, socket, servletInputStream);
		final Http1Response response = new Http1Response(out);

		container.service(request, response);
		response.close();
	}

	private ServletInputStream createServletStream(final Fields fields) {
		final String contentLength = fields.getField("content-length");

		if (contentLength == null) {
			return new LengthInputStream(null, 0L);
		}

		final long length;
		try {
			length = Long.parseLong(contentLength.toString());
		} catch (final NumberFormatException ex) {
			// TODO: Bad Request
			return new LengthInputStream(null, 0L);
		}

		return new LengthInputStream(in, length);
	}

	private Fields readFields() throws IOException {
		final List<Field> fieldList = new ArrayList<>();
		String fieldLine;
		while (!(fieldLine = readLine()).isEmpty()) {
			fieldList.add(Field.parse(fieldLine));
		}
		return new Fields(fieldList);
	}

	private String readLine() throws IOException {
		final StringBuilder sb = new StringBuilder();
		char lastCh = 0;
		while (true) {
			int ch = in.read();
			if (ch == -1) {
				throw new EOFException();
			}
			if (lastCh == '\r' && ch == '\n') {
				return sb.toString();
			}
			if (lastCh != 0)
				sb.append(lastCh);
			lastCh = (char) ch;
		}
	}
}
