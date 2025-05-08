package se.narstrom.test.servlet.http.exceptions;

import java.io.Serial;

public abstract class HttpStatusCodeException extends Exception
{
   @Serial
   private static final long serialVersionUID = 1L;

   private final int statusCode;

   protected HttpStatusCodeException(final int statusCode, final String message)
   {
      super(message);
      this.statusCode = statusCode;
   }

   protected HttpStatusCodeException(final int statusCode, final String message, final Throwable cause)
   {
      super(message, cause);
      this.statusCode = statusCode;
   }

   public int getStatusCode()
   {
      return statusCode;
   }
}
