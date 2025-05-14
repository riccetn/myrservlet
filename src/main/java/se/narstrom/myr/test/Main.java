package se.narstrom.myr.test;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.concurrent.Executors;

import jakarta.servlet.ServletRegistration;
import se.narstrom.myr.http.v1.Http1WorkerFactory;
import se.narstrom.myr.net.Server;
import se.narstrom.myr.servlet.Container;
import se.narstrom.myr.servlet.Context;

public final class Main {
	public static void main(final String[] args) throws Exception {
		final Context context = new Context(Path.of("C:\\webroot"));

		final ServletRegistration helloRegistration = context.addServlet("Hello", new HelloServlet());
		helloRegistration.addMapping("/hello");

		final ServletRegistration goodByteRegistration = context.addServlet("Good Bye", new GoodByeServlet());
		goodByteRegistration.addMapping("/goodbye");

		try (final Container container = new Container(context)) {
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
