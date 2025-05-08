package se.narstrom.test.servlet;

import se.narstrom.test.servlet.http.v1.Http1WorkerFactory;
import se.narstrom.test.servlet.net.Server;
import se.narstrom.test.servlet.servlet.Container;
import se.narstrom.test.servlet.servlet.Context;
import se.narstrom.test.servlet.servlet.TestServlet;

import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;

public final class Main
{
   public static void main(final String[] args) throws Exception
   {
      try (final Container container = new Container(new Context(Path.of("C:\\webroot"), Map.of(), new TestServlet())))
      {
         container.init();
         try (final Server server = new Server(new ServerSocket(8080), Executors.newVirtualThreadPerTaskExecutor(), new Http1WorkerFactory(container)))
         {
            server.run();
         }
      }
   }
}
