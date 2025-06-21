package se.narstrom.myr.http.v1;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import se.narstrom.myr.http.semantics.Field;
import se.narstrom.myr.http.semantics.Fields;

public final class Http1InputStream extends FilterInputStream {
	public Http1InputStream(final InputStream in) {
		super(in);
	}

	public String readLine() throws IOException {
		final StringBuilder sb = new StringBuilder();
		char lastCh = 0;
		while (true) {
			int ch = read();
			if (ch == -1) {
				throw new EOFException();
			}
			if (lastCh == '\r' && ch == '\n') {
				return sb.toString();
			}
			if (lastCh != 0)
				sb.append(lastCh);
			lastCh = (char) ch;
		}
	}

	public Fields readFields() throws IOException {
		final List<Field> fieldList = new ArrayList<>();
		String fieldLine;
		while (!(fieldLine = readLine()).isEmpty()) {
			fieldList.add(Field.parse(fieldLine));
		}
		return new Fields(fieldList);
	}
}
