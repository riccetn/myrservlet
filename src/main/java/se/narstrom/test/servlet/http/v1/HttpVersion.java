package se.narstrom.test.servlet.http.v1;

public record HttpVersion(int major, int minor)
{
   public static HttpVersion parse(final String versionString)
   {
      if (!versionString.startsWith("HTTP/"))
         throw new IllegalArgumentException("Invalid HTTP version: " + versionString);

      final String[] parts = versionString.substring(5).split("\\.");
      if (parts.length != 2)
         throw new IllegalArgumentException("Invalid HTTP version: " + versionString);

      try
      {
         return new HttpVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
      }
      catch (final NumberFormatException ex)
      {
         throw new IllegalArgumentException("Invalid HTTP version: " + versionString, ex);
      }
   }
}
