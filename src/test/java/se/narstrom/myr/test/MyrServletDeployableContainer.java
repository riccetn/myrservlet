package se.narstrom.myr.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
import se.narstrom.myr.servlet.session.SessionManager;

public class MyrServletDeployableContainer implements DeployableContainer<MyrServletContainerConfiguration> {

	private Path base;

	private Path temproot;

	private Server server;

	private Container container;

	private SessionManager sessionManager;

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
			base = Files.createTempDirectory("webroot");
			temproot = Files.createTempDirectory("temproot");

			container = new Container();
			server = new Server(new ServerSocket(8080), Executors.newVirtualThreadPerTaskExecutor(), new Http1WorkerFactory(container));
			thread = new Thread(server);

			sessionManager = new SessionManager();

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

		Exception ex = null;
		try {
			server.close();
		} catch (final Exception ex2) {
			ex = ex2;
		}

		try {
			thread.join();
		} catch (final InterruptedException _) {
			Thread.currentThread().interrupt();
		}

		try {
			Files.delete(base);
		} catch (final IOException ex2) {
			if (ex != null)
				ex.addSuppressed(ex2);
			else
				ex = ex2;
		}

		server = null;
		thread = null;
		base = null;

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
		final Path tempPath = temproot.resolve(name);

		archive.as(ZipExporter.class).exportTo(archivePath.toFile());

		try {
			ZipUtils.extract(deploymentPath, archivePath);
		} catch (final IOException ex) {
			throw new DeploymentException("Could not extract archive", ex);
		}

		try {
			Files.createDirectories(tempPath);
		} catch (final IOException ex) {
			throw new DeploymentException("Could not create temp directory", ex);
		}

		final String contextPath = "/" + name;

		final Context context;
		try {
			context = Deployer.deploy(contextPath, deploymentPath, sessionManager);
			context.init();
		} catch (final IOException ex) {
			throw new DeploymentException(name, ex);
		}

		context.setAttribute("jakarta.servlet.context.tempdir", tempPath.toFile());

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

		final String archiveName = archive.getName();
		if (!archiveName.endsWith(".war"))
			throw new DeploymentException("Not a WAR file " + archiveName);

		final Path archivePath = base.resolve(archiveName);

		final String name = archiveName.substring(0, archiveName.length() - 4);
		final Path deploymentPath = base.resolve(name);
		final Path tempPath = temproot.resolve(name);

		final String contextPath = "/" + name;

		final Context context = container.removeContext(contextPath);
		context.destroy();

		try {
			Files.delete(archivePath);
		} catch (final IOException ex) {
			ex.printStackTrace();
		}

		deleteTree(deploymentPath);
		deleteTree(tempPath);
	}

	private void deleteTree(final Path root) {
		try {
			Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					try {
						Files.delete(file);
					} catch (IOException ex) {
						ex.printStackTrace();
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(final Path file, final IOException ex) throws IOException {
					ex.printStackTrace();
					try {
						Files.delete(file);
					} catch (final IOException ex2) {
						ex2.printStackTrace();
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(final Path dir, final IOException ex) throws IOException {
					if (ex != null)
						ex.printStackTrace();
					try {
						Files.delete(dir);
					} catch (final IOException ex2) {
						ex2.printStackTrace();
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}

}
