package se.narstrom.myr.servlet.deploy;

public record ServletVersion(int major, int minor) {

	public static ServletVersion parse(final String str) {
		final int dot = str.indexOf('.');
		final int major = Integer.parseUnsignedInt(str, 0, dot, 10);
		final int minor = Integer.parseUnsignedInt(str, dot + 1, str.length(), 10);
		return new ServletVersion(major, minor);
	}
}
