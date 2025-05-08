package se.narstrom.test.servlet.net;

import java.io.IOException;
import java.net.Socket;

@FunctionalInterface
public interface ServerClientWorkerFactory
{
   ServerClientWorker createWorker(final Socket socket) throws IOException;
}
