package se.narstrom.myr;

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
			try (final Server server = new Server(new ServerSocket(8080), Executors.newVirtualThreadPerTaskExecutor(), new Http1WorkerFactory(container))) {
				server.run();
			}
		}
	}
}
