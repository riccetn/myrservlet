package se.narstrom.test.servlet.http.v1;

import se.narstrom.test.servlet.uri.Segment;

import java.util.List;
import java.util.stream.Stream;

public record AbsolutePath(List<Segment> segments)
{
   public static AbsolutePath parse(final String str)
   {
      if (!str.startsWith("/"))
         throw new IllegalArgumentException("Invalid absolute path: " + str);
      final String[] parts = str.substring(1).split("/");
      return new AbsolutePath(Stream.of(parts).map(Segment::new).toList());
   }
}
