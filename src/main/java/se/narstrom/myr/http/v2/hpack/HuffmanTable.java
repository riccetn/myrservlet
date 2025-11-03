package se.narstrom.myr.http.v2.hpack;

public class HuffmanTable {
	public static final int BASE_5BIT = 0x0000_0000;
	public static final char[] TABLE_5BIT = { '0', '1', '2', 'a', 'c', 'e', 'i', 'o', 's', 't' };

	public static final int BASE_6BIT = 0x6000_0000;
	public static final char[] TABLE_6BIT = { ' ', '%', '-', '.', '/', '3', '4', '5', '6', '7', '8', '9', '=', 'A', '_', 'b', 'd', 'f', 'g', 'h', 'l', 'm', 'n', 'p', 'r', 'u' };

	public static final int BASE_7BIT = 0xB800_0000;
	public static final char[] TABLE_7BIT = { ':', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'Y', 'j', 'k', 'q', 'v', 'w', 'x', 'y',
			'z' };

	public static final int BASE_8BIT = 0xf800_0000;
	public static final char[] TABLE_8BIT = { '&', '*', ',', ';', 'X', 'Z' };

	public static final int BASE_10BIT = 0xfe00_0000;
	public static final char[] TABLE_10BIT = { '!', '\"', '(', ')', '?' };

	public static final int BASE_11BIT = 0xff40_0000;
	public static final char[] TABLE_11BIT = { '\'', '+', '|' };

	public static final int BASE_12BIT = 0xffa0_0000;
	public static final char[] TABLE_12BIT = { '#', '>' };

	public static final int BASE_13BIT = 0xffc0_0000;
	public static final char[] TABLE_13BIT = { '\0', '$', '@', '[', ']', '~' };

	public static final int BASE_14BIT = 0xfff0_0000;
	public static final char[] TABLE_14BIT = { '^', '}' };

	public static final int BASE_15BIT = 0xfff8_0000;
	public static final char[] TABLE_15BIT = { '<', '`', '{' };

	public static final int BASE_19BIT = 0xfffe_0000;
	public static final char[] TABLE_19BIT = { '\\', 195, 208 };
}
