package se.narstrom.myr.http.v1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import se.narstrom.myr.http.exceptions.HttpStatusCodeException;
import se.narstrom.myr.http.semantics.Fields;
import se.narstrom.myr.net.ServerClientWorker;
import se.narstrom.myr.servlet.container.Container;

public final class Http1Worker implements ServerClientWorker {
	private final Container container;

	private final Socket socket;

	private final Http1InputStream in;

	private final OutputStream out;

	public Http1Worker(final Container container, final Socket socket) throws IOException {
		this.container = container;
		this.socket = socket;
		this.in = new Http1InputStream(socket.getInputStream());
		this.out = socket.getOutputStream();
	}

	@Override
	public void run() throws IOException {
		final RequestLine requestLine = RequestLine.parse(in.readLine());
		final Fields headerFields = in.readFields();

		final InputStream servletInputStream = createServletStream(headerFields);

		final Http1Request request = new Http1Request(requestLine.method(), requestLine.target().absolutePath(), requestLine.target().query(), headerFields, socket, servletInputStream);
		final Http1Response response = new Http1Response(out);

		try {
			container.service(request, response);
		} catch (final HttpStatusCodeException ex) {
			if(!response.isCommitted()) {
				response.setStatus(ex.getStatusCode());
				response.setHeader("connection", "close");
			}
		}
		response.close();
	}

	private InputStream createServletStream(final Fields fields) {
		final String contentLength = fields.getField("content-length");
		if (contentLength != null) {
			final long length;
			try {
				length = Long.parseLong(contentLength.toString());
			} catch (final NumberFormatException ex) {
				// TODO: Bad Request
				return new LengthInputStream(null, 0L);
			}

			return new LengthInputStream(in, length);
		}

		final String transferEncoding = fields.getField("transfer-encoding");
		if ("chunked".equals(transferEncoding)) {
			return new ChunkedInputStream(in);
		}

		return new LengthInputStream(null, 0L);
	}
}
