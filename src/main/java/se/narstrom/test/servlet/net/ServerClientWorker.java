package se.narstrom.test.servlet.net;

import java.io.IOException;

@FunctionalInterface
public interface ServerClientWorker
{
   void run() throws IOException;
}
