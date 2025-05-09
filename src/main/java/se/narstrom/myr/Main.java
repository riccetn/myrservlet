package se.narstrom.myr;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;

import se.narstrom.myr.http.v1.Http1WorkerFactory;
import se.narstrom.myr.net.Server;
import se.narstrom.myr.servlet.Container;
import se.narstrom.myr.servlet.Context;
import se.narstrom.myr.servlet.TestServlet;

public final class Main {
	public static void main(final String[] args) throws Exception {
		try (final Container container = new Container(new Context(Path.of("C:\\webroot"), Map.of(), new TestServlet()))) {
			container.init();
			final ServerSocket socket = new ServerSocket();
			socket.setReuseAddress(true);
			socket.bind(new InetSocketAddress(8080));
			try (final Server server = new Server(socket, Executors.newVirtualThreadPerTaskExecutor(), new Http1WorkerFactory(container))) {
				server.run();
			}
		}
	}
}
