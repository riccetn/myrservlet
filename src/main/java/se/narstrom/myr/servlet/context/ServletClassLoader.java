package se.narstrom.myr.servlet.context;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public final class ServletClassLoader extends ClassLoader {
	private final Context context;

	private final Path base;

	public ServletClassLoader(final Context context, final Path base, final ClassLoader parent) {
		super(parent);
		this.context = context;
		this.base = base;
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
			final Path path = base.resolve("WEB-INF/classes", name);
			if (Files.exists(path))
				return path.toUri().toURL();

			final Path lib = base.resolve("lib");
			try (final DirectoryStream<Path> jars = Files.newDirectoryStream(lib)) {
				for (final Path jar : jars) {
					if (!jar.getFileName().toString().endsWith(".jar"))
						continue;

					try (final FileSystem fs = FileSystems.newFileSystem(jar)) {
						final Path resource = fs.getPath(name);
						if (Files.exists(resource))
							return resource.toUri().toURL();
					}
				}
			}

			return null;
		} catch (final IOException ex) {
			return null;
		}
	}

	@Override
	protected Enumeration<URL> findResources(final String name) throws IOException {
		final List<URL> urls = new ArrayList<>();
		try {
			final Path path = base.resolve("WEB-INF/classes", name);
			if (Files.exists(path))
				urls.add(path.toUri().toURL());

			final Path lib = base.resolve("lib");
			try (final DirectoryStream<Path> jars = Files.newDirectoryStream(lib)) {
				for (final Path jar : jars) {
					if (!jar.getFileName().toString().endsWith(".jar"))
						continue;

					try (final FileSystem fs = FileSystems.newFileSystem(jar)) {
						final Path resource = fs.getPath(name);
						if (Files.exists(resource))
							urls.add(resource.toUri().toURL());
					}
				}
			}
		} catch (final IOException ex) {
		}
		return Collections.enumeration(urls);
	}
}
