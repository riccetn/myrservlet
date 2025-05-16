package se.narstrom.myr.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

import jakarta.servlet.ServletException;
import se.narstrom.myr.http.v1.Http1WorkerFactory;
import se.narstrom.myr.net.Server;
import se.narstrom.myr.servlet.Container;
import se.narstrom.myr.servlet.Context;
import se.narstrom.myr.servlet.Deployer;

public final class Test {
	public static void main(final String[] args) throws IOException, ServletException {
		final Container container = new Container();
		container.init();

		final Context testContext = Deployer.deploy("/servlet_js_servlet_web", Paths.get("C:\\Development\\webroot\\servlet_js_servlet_web"));
		testContext.init();
		container.addContext(testContext);

		final Server server = new Server(new ServerSocket(8080), Executors.newVirtualThreadPerTaskExecutor(), new Http1WorkerFactory(container));
		server.run();
	}
}
