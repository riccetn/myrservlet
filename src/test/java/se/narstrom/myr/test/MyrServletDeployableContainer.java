package se.narstrom.myr.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

import se.narstrom.myr.test.MyrServletTestServer.Protocol;

/**
 * Arquillian container that runs the servlet container in a separate JVM (see
 * {@link MyrServletTestServer}) and controls it over a local socket.
 */
public class MyrServletDeployableContainer implements DeployableContainer<MyrServletContainerConfiguration> {

	private MyrServletContainerConfiguration configuration;

	private Path archiveDir;

	private Process process;

	private Thread outputPump;

	private Socket control;

	private DataInputStream controlIn;

	private DataOutputStream controlOut;

	@Override
	public Class<MyrServletContainerConfiguration> getConfigurationClass() {
		return MyrServletContainerConfiguration.class;
	}

	@Override
	public void setup(final MyrServletContainerConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public ProtocolDescription getDefaultProtocol() {
		return new ProtocolDescription("Servlet 3.0");
	}

	@Override
	public void start() throws LifecycleException {
		System.out.println("start()");
		if (configuration == null)
			configuration = new MyrServletContainerConfiguration();

		try (final ServerSocket controlServer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
			archiveDir = Files.createTempDirectory("archives");

			process = launch(controlServer.getLocalPort());

			outputPump = Thread.ofPlatform().daemon().name("MyrServlet server output").start(() -> pump(process.getInputStream()));

			controlServer.setSoTimeout(configuration.getTimeout());
			control = acceptControlConnection(controlServer);
			control.setSoTimeout(configuration.getTimeout());
			controlIn = new DataInputStream(new BufferedInputStream(control.getInputStream()));
			controlOut = new DataOutputStream(new BufferedOutputStream(control.getOutputStream()));

			final String ready = controlIn.readUTF();
			if (!Protocol.READY.equals(ready))
				throw new IOException("Unexpected message from server: " + ready);

			// Deployments may take a while; only start and stop are bounded by the timeout.
			control.setSoTimeout(0);
		} catch (final IOException ex) {
			destroyProcess();
			throw new LifecycleException("Could not start server JVM", ex);
		}
	}

	private Process launch(final int controlPort) throws IOException {
		final Path java = Path.of(System.getProperty("java.home"), "bin", "java");

		final List<String> command = new ArrayList<>();
		command.add(java.toString());
		for (final String arg : configuration.getJavaVmArguments().trim().split("\\s+")) {
			if (!arg.isEmpty())
				command.add(arg);
		}
		command.add(MyrServletTestServer.class.getName());
		command.add(Integer.toString(configuration.getHttpPort()));
		command.add(Integer.toString(controlPort));

		final ProcessBuilder builder = new ProcessBuilder(command);
		// Pass the class path through the environment to avoid command line length limits
		builder.environment().put("CLASSPATH", testClassPath());
		builder.redirectErrorStream(true);
		builder.redirectInput(ProcessBuilder.Redirect.PIPE);
		return builder.start();
	}

	private static String testClassPath() {
		// Surefire may put the real class path in a manifest-only jar
		final String surefireClassPath = System.getProperty("surefire.test.class.path");
		if (surefireClassPath != null && !surefireClassPath.isBlank())
			return surefireClassPath;
		return System.getProperty("java.class.path");
	}

	private Socket acceptControlConnection(final ServerSocket controlServer) throws IOException {
		final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(configuration.getTimeout());
		controlServer.setSoTimeout(500);
		while (true) {
			try {
				return controlServer.accept();
			} catch (final SocketTimeoutException ex) {
				if (!process.isAlive())
					throw new IOException("Server JVM exited with code " + process.exitValue() + " before connecting");
				if (System.nanoTime() > deadline)
					throw new IOException("Timed out waiting for server JVM to connect", ex);
			}
		}
	}

	/**
	 * Copies the server JVM's output to {@link System#out} so that it ends up
	 * wherever the test runner captures output.
	 */
	private static void pump(final InputStream in) {
		final byte[] buffer = new byte[8192];
		try (in) {
			int n;
			while ((n = in.read(buffer)) != -1) {
				System.out.write(buffer, 0, n);
				System.out.flush();
			}
		} catch (final IOException _) {
			// Process ended
		}
	}

	@Override
	public void stop() throws LifecycleException {
		System.out.println("stop()");

		Exception ex = null;
		try {
			control.setSoTimeout(configuration.getTimeout());
			controlOut.writeUTF(Protocol.STOP);
			controlOut.flush();
			expectOk();
		} catch (final Exception ex2) {
			ex = ex2;
		}

		try {
			control.close();
		} catch (final IOException ex2) {
			ex = addSuppressed(ex, ex2);
		}

		try {
			if (!process.waitFor(configuration.getTimeout(), TimeUnit.MILLISECONDS)) {
				ex = addSuppressed(ex, new IOException("Server JVM did not exit in time, killing it"));
				destroyProcess();
			}
			outputPump.join(configuration.getTimeout());
		} catch (final InterruptedException _) {
			Thread.currentThread().interrupt();
			destroyProcess();
		}

		try {
			Files.deleteIfExists(archiveDir);
		} catch (final IOException ex2) {
			ex = addSuppressed(ex, ex2);
		}

		process = null;
		outputPump = null;
		control = null;
		controlIn = null;
		controlOut = null;
		archiveDir = null;

		if (ex != null)
			throw new LifecycleException("Exception stoping container", ex);
	}

	private static Exception addSuppressed(final Exception ex, final Exception ex2) {
		if (ex == null)
			return ex2;
		ex.addSuppressed(ex2);
		return ex;
	}

	private void destroyProcess() {
		if (process == null)
			return;
		process.destroyForcibly();
		try {
			process.waitFor();
		} catch (final InterruptedException _) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public ProtocolMetaData deploy(final Archive<?> archive) throws DeploymentException {
		System.out.println("deploy(): " + archive.getName());

		final String archiveName = archive.getName();
		if (!archiveName.endsWith(".war"))
			throw new DeploymentException("Not a WAR file " + archiveName);

		final String name = archiveName.substring(0, archiveName.length() - 4);
		final String contextPath = "/" + name;
		final Path archivePath = archiveDir.resolve(archiveName);

		archive.as(ZipExporter.class).exportTo(archivePath.toFile(), true);

		final HTTPContext httpMetaData = new HTTPContext("localhost", configuration.getHttpPort());
		try {
			controlOut.writeUTF(Protocol.DEPLOY);
			controlOut.writeUTF(archivePath.toAbsolutePath().toString());
			controlOut.flush();
			expectOk();

			final int count = controlIn.readInt();
			for (int i = 0; i < count; ++i) {
				final String servletName = controlIn.readUTF();
				httpMetaData.add(new Servlet(servletName, contextPath));
			}
		} catch (final IOException ex) {
			throw new DeploymentException("Could not deploy " + name, ex);
		} finally {
			try {
				Files.deleteIfExists(archivePath);
			} catch (final IOException ex) {
				ex.printStackTrace();
			}
		}

		return new ProtocolMetaData().addContext(httpMetaData);
	}

	@Override
	public void undeploy(final Archive<?> archive) throws DeploymentException {
		System.out.println("undeploy(): " + archive.getName());

		final String archiveName = archive.getName();
		if (!archiveName.endsWith(".war"))
			throw new DeploymentException("Not a WAR file " + archiveName);

		final String name = archiveName.substring(0, archiveName.length() - 4);

		try {
			controlOut.writeUTF(Protocol.UNDEPLOY);
			controlOut.writeUTF(name);
			controlOut.flush();
			expectOk();
		} catch (final IOException ex) {
			throw new DeploymentException("Could not undeploy " + name, ex);
		}
	}

	private void expectOk() throws IOException {
		final String status = controlIn.readUTF();
		if (Protocol.OK.equals(status))
			return;
		if (Protocol.ERROR.equals(status))
			throw new IOException("Server JVM reported: " + controlIn.readUTF());
		throw new IOException("Unexpected reply from server JVM: " + status);
	}

}
