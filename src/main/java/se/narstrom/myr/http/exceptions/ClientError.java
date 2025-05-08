package se.narstrom.myr.http.exceptions;

public abstract class ClientError extends HttpStatusCodeException
{
   protected ClientError(final int statusCode, final String message)
   {
      super(statusCode, message);
   }

   protected ClientError(final int statusCode, final String message, final Throwable cause)
   {
      super(statusCode, message, cause);
      if (statusCode / 100 != 4)
         throw new IllegalArgumentException("Invalid status code: " + statusCode);
   }
}
