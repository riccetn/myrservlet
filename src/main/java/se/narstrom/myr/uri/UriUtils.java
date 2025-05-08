package se.narstrom.myr.uri;

import static se.narstrom.myr.AugmentedBackusNaurFormUtils.*;

public final class UriUtils
{
   public static boolean isPathChar(char ch)
   {
      return isUnreserved(ch) || isSubDelim(ch) || ch == ':' || ch == '@';
   }

   public static boolean isUnreserved(char ch)
   {
      return isAlpha(ch) || isDigit(ch) || ch == '-' || ch == '.' || ch == '_' || ch == '~';
   }

   public static boolean isPercentEncoded(final String str)
   {
      if (str.length() != 3)
         return false;
      final char[] chs = str.toCharArray();
      return (chs[0] == '%') && isHexDigit(chs[1]) && isHexDigit(chs[2]);
   }

   public static boolean isSubDelim(char ch)
   {
      return (ch == '!' || ch == '$' || ch == '&' || ch == '\'' || ch == '(' || ch == ')' || ch == '*' || ch == '+' || ch == ',' || ch == ';' || ch == '=');
   }
}
