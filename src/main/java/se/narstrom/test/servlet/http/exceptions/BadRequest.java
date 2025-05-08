package se.narstrom.test.servlet.http.exceptions;

public final class BadRequest extends ClientError
{
   public BadRequest(String message)
   {
      super(400, message);
   }

   public BadRequest(String message, Throwable cause)
   {
      super(400, message, cause);
   }
}
