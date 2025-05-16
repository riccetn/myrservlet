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
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

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

			container = new Container();
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

		final String archiveName = archive.getName();
		if (!archiveName.endsWith(".war"))
			throw new DeploymentException("Not a WAR file " + archiveName);

		final Path archivePath = base.resolve(archiveName);

		final String name = archiveName.substring(0, archiveName.length() - 4);
		final Path deploymentPath = base.resolve(name);

		archive.as(ZipExporter.class).exportTo(archivePath.toFile());

		try {
			ZipUtils.extract(deploymentPath, archivePath);
		} catch (final IOException ex) {
			throw new DeploymentException("Could not extract archive", ex);
		}

		final String contextPath = "/" + name;

		final Context context;
		try {
			context = Deployer.deploy(contextPath, deploymentPath);
			context.init();
		} catch (final IOException ex) {
			throw new DeploymentException(name, ex);
		}

		container.addContext(context);

		final HTTPContext httpMetaData = new HTTPContext("localhost", 8080);

		for (final ServletRegistration registration : context.getServletRegistrations().values()) {
			System.out.println("Registration: " + registration.getName() + " " + contextPath);
			httpMetaData.add(new Servlet(registration.getName(), contextPath));
		}

		return new ProtocolMetaData().addContext(httpMetaData);
	}

	@Override
	public void undeploy(final Archive<?> archive) throws DeploymentException {
		System.out.println("undeploy(): " + archive.getName());
		// TODO: Not Implemented
	}

}
