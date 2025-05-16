package se.narstrom.myr.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ZipUtils {

	public static void extract(final Path target, final Path archivePath) throws IOException {
		try (final ZipFile archive = new ZipFile(archivePath.toFile())) {
			extract(target, archive);
		}
	}

	public static void extract(final Path target, final ZipFile archive) throws IOException {
		Files.createDirectories(target);
		for (final Iterator<? extends ZipEntry> iter = archive.entries().asIterator(); iter.hasNext();) {
			final ZipEntry entry = iter.next();

			final Path entryPath = target.resolve(entry.getName());

			if (entry.isDirectory()) {
				Files.createDirectories(entryPath);
			} else {
				Files.createDirectories(entryPath.getParent());
				Files.copy(archive.getInputStream(entry), entryPath);
			}
		}
	}
}
