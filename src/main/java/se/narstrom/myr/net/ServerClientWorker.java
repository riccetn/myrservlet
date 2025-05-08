package se.narstrom.myr.net;

import java.io.IOException;

@FunctionalInterface
public interface ServerClientWorker
{
   void run() throws IOException;
}
