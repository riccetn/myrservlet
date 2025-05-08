package se.narstrom.myr;

public final class AugmentedBackusNaurFormUtils
{
   public static boolean isDigit(final char ch)
   {
      return ch >= '0' && ch <= '9';
   }

   public static boolean isAlpha(final char ch)
   {
      return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
   }

   public static boolean isHexDigit(final char ch)
   {
      return isDigit(ch) || (ch >= 'A' && ch <= 'F') || (ch >= 'a' && ch <= 'f');
   }
}
