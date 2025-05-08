package se.narstrom.myr.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public final class ServletClassLoader extends ClassLoader {
	private final Context context;

	public ServletClassLoader(final Context context, final ClassLoader parent) {
		super(parent);
		this.context = context;
	}

	@Override
	protected Class<?> findClass(final String name) throws ClassNotFoundException {
		final InputStream in = context.getResourceAsStream(name.replace('.', '/') + ".class");
		if (in == null)
			throw new ClassNotFoundException(name);
		try {
			final byte[] bytes = in.readAllBytes();
			return defineClass(name, bytes, 0, bytes.length);
		} catch (final IOException ex) {
			throw new ClassNotFoundException(name, ex);
		}
	}

	@Override
	protected URL findResource(final String name) {
		try {
			return context.getResource(name);
		} catch (final MalformedURLException _) {
			return null;
		}
	}

	@Override
	protected Enumeration<URL> findResources(final String name) throws IOException {
		final URL url = findResource(name);
		if (url != null)
			return Collections.enumeration(List.of(url));
		else
			return Collections.emptyEnumeration();
	}
}
