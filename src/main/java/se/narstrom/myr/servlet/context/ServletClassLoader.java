package se.narstrom.myr.servlet.context;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ServletClassLoader extends URLClassLoader {
	public ServletClassLoader(final Context context, final Path base, final ClassLoader parent) {
		final List<URL> urls = new ArrayList<>();
		try {
			urls.add(base.resolve("WEB-INF/classes").toUri().toURL());
			final Path libDir = base.resolve("WEB-INF/lib");
			if (Files.isDirectory(libDir)) {
				try (final DirectoryStream<Path> dir = Files.newDirectoryStream(base.resolve("WEB-INF/lib"))) {
					for (final Path jar : dir) {
						if (!jar.getFileName().toString().endsWith(".jar"))
							continue;
						urls.add(URI.create("jar:" + jar.toUri().toASCIIString() + "!/").toURL());
					}
				}
			}
		} catch (final IOException ex) {
			throw new IllegalArgumentException(ex);
		}

		super(urls.toArray(URL[]::new), parent);
	}
}
