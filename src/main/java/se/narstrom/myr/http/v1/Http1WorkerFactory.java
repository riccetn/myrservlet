package se.narstrom.myr.http.v1;

import java.io.IOException;
import java.net.Socket;

import se.narstrom.myr.net.ServerClientWorker;
import se.narstrom.myr.net.ServerClientWorkerFactory;
import se.narstrom.myr.servlet.Container;
import se.narstrom.myr.servlet.Context;

public final class Http1WorkerFactory implements ServerClientWorkerFactory {
	private final Container container;

	public Http1WorkerFactory(final Container container) {
		this.container = container;
	}

	@Override
	public ServerClientWorker createWorker(final Socket socket) throws IOException {
		return new Http1Worker(container, socket);
	}
}
