package se.narstrom.myr.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import jakarta.servlet.ServletRegistration;
import se.narstrom.myr.http.v1.Http1WorkerFactory;
import se.narstrom.myr.net.Server;
import se.narstrom.myr.servlet.container.Container;
import se.narstrom.myr.servlet.context.Context;
import se.narstrom.myr.servlet.deploy.Deployer;
import se.narstrom.myr.servlet.session.SessionManager;

/**
 * Entry point for the servlet container when it is run in a separate JVM by
 * {@link MyrServletDeployableContainer}.
 *
 * <p>
 * Usage: {@code MyrServletTestServer <httpPort> <controlPort>}
 *
 * <p>
 * On startup the HTTP server is started on {@code httpPort}, after which a
 * connection is made to the control socket on {@code localhost:controlPort}.
 * Commands and replies are exchanged as strings written with
 * {@link DataOutputStream#writeUTF(String)}; see {@link Protocol}.
 *
 * <p>
 * If the control connection is lost the server shuts down, so it never
 * outlives the test JVM that launched it.
 */
public final class MyrServletTestServer {

	static final class Protocol {
		static final String READY = "READY";
		static final String DEPLOY = "DEPLOY";
		static final String UNDEPLOY = "UNDEPLOY";
		static final String STOP = "STOP";
		static final String OK = "OK";
		static final String ERROR = "ERROR";

		private Protocol() {
		}
	}

	private final Path base;

	private final Path temproot;

	private final Container container;

	private final SessionManager sessionManager;

	private final Server server;

	private final Thread serverThread;

	private MyrServletTestServer(final int httpPort) throws Exception {
		base = Files.createTempDirectory("webroot");
		temproot = Files.createTempDirectory("temproot");

		container = new Container();
		sessionManager = new SessionManager();
		server = new Server(new ServerSocket(httpPort), Executors.newVirtualThreadPerTaskExecutor(), new Http1WorkerFactory(container));
		serverThread = new Thread(server, "Server");

		container.init();
		serverThread.start();
	}

	public static void main(final String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: MyrServletTestServer <httpPort> <controlPort>");
			System.exit(2);
		}

		final int httpPort = Integer.parseInt(args[0]);
		final int controlPort = Integer.parseInt(args[1]);

		final MyrServletTestServer testServer = new MyrServletTestServer(httpPort);
		try (final Socket control = new Socket(InetAddress.getLoopbackAddress(), controlPort)) {
			final DataInputStream in = new DataInputStream(new BufferedInputStream(control.getInputStream()));
			final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(control.getOutputStream()));

			out.writeUTF(Protocol.READY);
			out.flush();

			testServer.serve(in, out);
		} finally {
			testServer.shutdown();
		}

		System.exit(0);
	}

	private void serve(final DataInputStream in, final DataOutputStream out) throws IOException {
		while (true) {
			final String command;
			try {
				command = in.readUTF();
			} catch (final EOFException _) {
				System.err.println("Control connection closed, shutting down");
				return;
			}

			try {
				switch (command) {
					case Protocol.DEPLOY -> {
						final List<String> servletNames = deploy(Path.of(in.readUTF()));
						out.writeUTF(Protocol.OK);
						out.writeInt(servletNames.size());
						for (final String servletName : servletNames)
							out.writeUTF(servletName);
					}
					case Protocol.UNDEPLOY -> {
						undeploy(in.readUTF());
						out.writeUTF(Protocol.OK);
					}
					case Protocol.STOP -> {
						out.writeUTF(Protocol.OK);
						out.flush();
						return;
					}
					default -> throw new IllegalArgumentException("Unknown command: " + command);
				}
			} catch (final Exception ex) {
				ex.printStackTrace();
				out.writeUTF(Protocol.ERROR);
				out.writeUTF(String.valueOf(ex));
			}
			out.flush();
		}
	}

	private List<String> deploy(final Path archivePath) throws IOException {
		final String archiveName = archivePath.getFileName().toString();
		if (!archiveName.endsWith(".war"))
			throw new IllegalArgumentException("Not a WAR file " + archiveName);

		final String name = archiveName.substring(0, archiveName.length() - 4);
		final Path deploymentPath = base.resolve(name);
		final Path tempPath = temproot.resolve(name);

		ZipUtils.extract(deploymentPath, archivePath);
		Files.createDirectories(tempPath);

		final String contextPath = "/" + name;

		final Context context = new Deployer().deploy(contextPath, deploymentPath, sessionManager, container);
		context.init();
		context.setAttribute("jakarta.servlet.context.tempdir", tempPath.toFile());

		container.addContext(context);

		final List<String> servletNames = new ArrayList<>();
		for (final ServletRegistration registration : context.getServletRegistrations().values()) {
			System.out.println("Registration: " + registration.getName() + " " + contextPath);
			servletNames.add(registration.getName());
		}
		return servletNames;
	}

	private void undeploy(final String name) {
		final Context context = container.removeContext("/" + name);
		if (context != null)
			context.destroy();

		deleteTree(base.resolve(name));
		deleteTree(temproot.resolve(name));
	}

	private void shutdown() {
		serverThread.interrupt();
		try {
			server.close();
		} catch (final Exception ex) {
			ex.printStackTrace();
		}

		try {
			serverThread.join();
		} catch (final InterruptedException _) {
			Thread.currentThread().interrupt();
		}

		deleteTree(base);
		deleteTree(temproot);
	}

	private static void deleteTree(final Path root) {
		if (!Files.exists(root))
			return;
		try {
			Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
					delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(final Path file, final IOException ex) {
					ex.printStackTrace();
					delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(final Path dir, final IOException ex) {
					if (ex != null)
						ex.printStackTrace();
					delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}

	private static void delete(final Path path) {
		try {
			Files.delete(path);
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}
}
