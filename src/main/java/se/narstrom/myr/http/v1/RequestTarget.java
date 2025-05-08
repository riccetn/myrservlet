package se.narstrom.myr.http.v1;

import se.narstrom.myr.uri.Query;

public record RequestTarget(AbsolutePath absolutePath, Query query)
{
   public static RequestTarget parse(final String str)
   {
      final int queryIndex = str.indexOf('?');
      if (queryIndex == -1)
         return new RequestTarget(AbsolutePath.parse(str), new Query(""));
      else
         return new RequestTarget(AbsolutePath.parse(str.substring(0, queryIndex)), new Query(str.substring(queryIndex + 1)));
   }
}
