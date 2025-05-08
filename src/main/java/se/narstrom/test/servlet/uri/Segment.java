package se.narstrom.test.servlet.uri;

import static se.narstrom.test.servlet.AugmentedBackusNaurFormUtils.isHexDigit;

public record Segment(String value)
{
   public Segment
   {
      if (!isSegment(value))
         throw new IllegalArgumentException("Invalid segment: " + value);
   }

   public static boolean isSegment(final String segment)
   {
      final char[] chs = segment.toCharArray();
      int i = 0;
      while (i < chs.length)
      {
         if (chs[i] == '%')
         {
            if (chs.length - i < 3)
               return false;
            if (!isHexDigit(chs[i + 1]) || !isHexDigit(chs[i + 2]))
               return false;
            i += 3;
         }
         else
         {
            if (!UriUtils.isPathChar(chs[i]))
               return false;
            i += 1;
         }
      }
      return true;
   }
}
