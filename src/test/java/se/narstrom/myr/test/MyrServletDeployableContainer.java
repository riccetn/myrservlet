package se.narstrom.myr.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import se.narstrom.myr.http.v1.Http1WorkerFactory;
import se.narstrom.myr.net.Server;
import se.narstrom.myr.servlet.Container;
import se.narstrom.myr.servlet.Context;
import se.narstrom.myr.servlet.Deployer;

public class MyrServletDeployableContainer implements DeployableContainer<MyrServletContainerConfiguration> {

	private final Path base = Paths.get("C:\\Development\\webroot");

	private Server server;

	private Container container;

	private Thread thread;

	@Override
	public Class<MyrServletContainerConfiguration> getConfigurationClass() {
		return MyrServletContainerConfiguration.class;
	}

	@Override
	public ProtocolDescription getDefaultProtocol() {
		return new ProtocolDescription("Servlet 6.1");
	}

	@Override
	public void start() throws LifecycleException {
		System.out.println("start()");
		try {
			Files.createDirectories(base);

			final Path rootContextPath = base.resolve("ROOT");
			Files.createDirectories(rootContextPath);

			final Context rootContext = new Context(rootContextPath);
			container = new Container(rootContext);
			server = new Server(new ServerSocket(8080), Executors.newVirtualThreadPerTaskExecutor(), new Http1WorkerFactory(container));
			thread = new Thread(server);

			container.init();
			thread.start();
		} catch (final ServletException | IOException ex) {
			throw new LifecycleException("IO Error", ex);
		}
	}

	@Override
	public void stop() throws LifecycleException {
		System.out.println("stop()");

		thread.interrupt();

		try {
			thread.join();
		} catch (final InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

		Exception ex = null;
		try {
			server.close();
		} catch (final Exception ex2) {
			ex = ex2;
		}

		server = null;
		thread = null;

		if (ex != null)
			throw new LifecycleException("Exception stoping container", ex);
	}

	@Override
	public ProtocolMetaData deploy(final Archive<?> archive) throws DeploymentException {
		System.out.println("deploy(): " + archive.getName());

		String name = archive.getName();
		if (name.endsWith(".war"))
			name = name.substring(0, name.length() - 4);

		final Path deploymentPath = archive.as(ExplodedExporter.class).exportExploded(base.toFile(), name).toPath();

		final Context context;
		try {
			context = Deployer.deploy(deploymentPath);
			context.init();
		} catch (final ServletException | IOException ex) {
			throw new DeploymentException(name, ex);
		}

		container.addContext(name, context);

		final HTTPContext httpMetaData = new HTTPContext("localhost", 8080);

		final String contextRoot = "/" + name;

		for (final ServletRegistration registration : context.getServletRegistrations().values()) {
			System.out.println("Registration: " + registration.getName() + " " + contextRoot);
			httpMetaData.add(new Servlet(registration.getName(), contextRoot));
		}

		return new ProtocolMetaData().addContext(httpMetaData);
	}

	@Override
	public void undeploy(final Archive<?> archive) throws DeploymentException {
		System.out.println("undeploy(): " + archive.getName());
		// TODO: Not Implemented
	}

}
