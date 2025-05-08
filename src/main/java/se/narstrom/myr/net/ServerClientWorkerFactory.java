package se.narstrom.myr.net;

import java.io.IOException;
import java.net.Socket;

@FunctionalInterface
public interface ServerClientWorkerFactory
{
   ServerClientWorker createWorker(final Socket socket) throws IOException;
}
