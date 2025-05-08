package se.narstrom.myr.http.v1;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import se.narstrom.myr.http.semantics.Field;
import se.narstrom.myr.http.semantics.Token;
import se.narstrom.myr.net.ServerClientWorker;
import se.narstrom.myr.servlet.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Http1Worker implements ServerClientWorker
{
   private final Container container;

   private final Socket socket;

   private final InputStream in;

   private final OutputStream out;

   public Http1Worker(final Container container, final Socket socket) throws IOException
   {
      this.container = container;
      this.socket = socket;
      this.in = new BufferedInputStream(socket.getInputStream());
      this.out = new BufferedOutputStream(socket.getOutputStream());
   }

   @Override
   public void run() throws IOException
   {
      final RequestLine requestLine = RequestLine.parse(readLine());
      final Map<Token, List<String>> fields = new HashMap<>();
      String fieldLine;
      while (!(fieldLine = readLine()).isEmpty())
      {
         final Field field = Field.parse(fieldLine);
         fields.computeIfAbsent(field.name(), _ -> new ArrayList<>()).add(field.value());
      }

      final MyrServletRequest request = new MyrServletRequest(requestLine.method(), requestLine.target().absolutePath(), requestLine.target().query(), fields, socket, in);
      final MyrServletResponse response = new MyrServletResponse(out);

      try
      {
         container.service(request, response);
         response.flushBuffer();
      }
      catch (final ServletException ex)
      {
         throw new IOException(ex);
      }
   }

   private String readLine() throws IOException
   {
      final StringBuilder sb = new StringBuilder();
      char lastCh = 0;
      while (true)
      {
         int ch = in.read();
         if (ch == -1)
         {
            throw new EOFException();
         }
         if (lastCh == '\r' && ch == '\n')
         {
            return sb.toString();
         }
         if (lastCh != 0)
            sb.append(lastCh);
         lastCh = (char) ch;
      }
   }
}
