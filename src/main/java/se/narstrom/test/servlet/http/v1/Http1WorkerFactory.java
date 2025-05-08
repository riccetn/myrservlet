package se.narstrom.test.servlet.http.v1;

import se.narstrom.test.servlet.net.ServerClientWorker;
import se.narstrom.test.servlet.net.ServerClientWorkerFactory;
import se.narstrom.test.servlet.servlet.Container;
import se.narstrom.test.servlet.servlet.Context;

import java.io.IOException;
import java.net.Socket;

public final class Http1WorkerFactory implements ServerClientWorkerFactory
{
   private final Container container;

   public Http1WorkerFactory(final Container container)
   {
      this.container = container;
   }

   @Override
   public ServerClientWorker createWorker(final Socket socket) throws IOException
   {
      return new Http1Worker(container, socket);
   }
}
