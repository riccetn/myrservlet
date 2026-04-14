package se.narstrom.myr.servlet;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import se.narstrom.myr.web.url.UrlEncoding;

// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#http-protocol-parameters
public final class Parameters {
	private final Map<String, String[]> map;

	@SuppressWarnings("unchecked")
	public Parameters(final Map<String, List<String>> parameters) {
		Objects.requireNonNull(parameters);
		final Map.Entry<String, String[]>[] entries = new Map.Entry[parameters.size()];
		int i = 0;
		for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
			entries[i++] = Map.entry(entry.getKey(), entry.getValue().toArray(String[]::new));
		}
		this.map = Map.ofEntries(entries);
	}

	public String getParameter(final String name) {
		Objects.requireNonNull(name);
		final String[] values = map.get(name);
		if (values == null || values.length == 0)
			return null;
		return values[0];
	}

	public Enumeration<String> getParameterNames() {
		return Collections.enumeration(map.keySet());
	}

	public String[] getParameterValues(final String name) {
		Objects.requireNonNull(name);
		final String[] values = map.get(name);
		if (values == null)
			return null;
		return values.clone();
	}

	public Map<String, String[]> getParameterMap() {
		@SuppressWarnings("unchecked")
		final Map.Entry<String, String[]>[] entries = new Map.Entry[map.size()];
		int i = 0;
		for (final Map.Entry<String, String[]> entry : map.entrySet()) {
			entries[i++] = Map.entry(entry.getKey(), entry.getValue().clone());
		}
		return Map.ofEntries(entries);
	}

	public static Parameters parseQueryOnly(final String query) {
		final Map<String, List<String>> parameters;

		if (query != null) {
			parameters = UrlEncoding.parseToMap(query);
		} else {
			parameters = Map.of();
		}

		return new Parameters(parameters);
	}

	public static Parameters parseUrlEncoded(final String query, final Reader body) {
		final HashMap<String, List<String>> parameters = new HashMap<>();

		if (query != null) {
			final Map<String, List<String>> queryParameters = UrlEncoding.parseToMap(query);
			for (final Map.Entry<String, List<String>> ent : queryParameters.entrySet()) {
				parameters.computeIfAbsent(ent.getKey(), _ -> new ArrayList<>()).addAll(ent.getValue());
			}
		}

		try {
			final String content = readAll(body);
			final Map<String, List<String>> contentParams = UrlEncoding.parseToMap(content);
			for (final Map.Entry<String, List<String>> ent : contentParams.entrySet()) {
				parameters.computeIfAbsent(ent.getKey(), _ -> new ArrayList<>()).addAll(ent.getValue());
			}
		} catch (final IOException ex) {
			throw new IllegalStateException(ex);
		}

		return new Parameters(parameters);
	}

	private static String readAll(final Reader reader) throws IOException {
		final StringWriter writer = new StringWriter();
		reader.transferTo(writer);
		return writer.toString();
	}
}
