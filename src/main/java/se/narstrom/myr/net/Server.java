package se.narstrom.myr.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Server implements Runnable, AutoCloseable {
	private final Logger logger = Logger.getLogger(getClass().getName());

	private final ServerSocket serverSocket;

	private final Executor executor;

	private final ServerClientWorkerFactory clientWorkerFactory;

	public Server(final ServerSocket serverSocket, final Executor executor, final ServerClientWorkerFactory clientWorkerFactory) {
		this.serverSocket = serverSocket;
		this.executor = executor;
		this.clientWorkerFactory = clientWorkerFactory;
	}

	@Override
	public void close() throws Exception {
		Exception ex = null;
		try {
			serverSocket.close();
		} catch (final Exception ex2) {
			ex = ex2;
		}

		if (executor instanceof AutoCloseable closeable) {
			try {
				closeable.close();
			} catch (final Exception ex2) {
				if (ex == null) {
					ex = ex2;
				} else {
					ex.addSuppressed(ex2);
				}
			}
		}

		if (clientWorkerFactory instanceof AutoCloseable closeable) {
			try {
				closeable.close();
			} catch (Exception ex2) {
				if (ex == null) {
					ex = ex2;
				} else {
					ex.addSuppressed(ex2);
				}
			}
		}

		if (ex != null) {
			throw ex;
		}
	}

	public void run() {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				final Socket socket = serverSocket.accept();
				executor.execute(() -> clientEntry(socket));
			}
		} catch (final SocketException ex) {
			if (ex.getMessage().equals("Socket closed"))
				return;
			ex.printStackTrace();
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}

	private void clientEntry(final Socket socket) {
		try {
			final String oldName = Thread.currentThread().getName();
			logger.info(() -> "Accepted connection from " + socket.getRemoteSocketAddress());
			try {
				Thread.currentThread().setName("Client " + socket.getLocalSocketAddress() + " <- " + socket.getRemoteSocketAddress());
				final ServerClientWorker worker = clientWorkerFactory.createWorker(socket);
				worker.run();
				socket.close();
			} finally {
				Thread.currentThread().setName(oldName);
			}
		} catch (final Exception ex) {
			logger.log(Level.SEVERE, ex, () -> "Error handling connection from " + socket.getRemoteSocketAddress());
		}
	}
}
