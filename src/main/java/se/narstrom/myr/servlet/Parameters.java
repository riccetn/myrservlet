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

import se.narstrom.myr.uri.UrlEncoding;

// https://jakarta.ee/specifications/servlet/6.1/jakarta-servlet-spec-6.1#http-protocol-parameters
public final class Parameters {
	private final Map<String, String[]> map;

	@SuppressWarnings("unchecked")
	public Parameters(final Map<String, List<String>> parameters) {
		final List<Map.Entry<String, String[]>> entries = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
			entries.add(Map.entry(entry.getKey(), entry.getValue().toArray(String[]::new)));
		}
		this.map = Map.ofEntries(entries.toArray(Map.Entry[]::new));
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
			return null; // NOSONAR: We are required by specification to return null
		return values.clone();
	}

	public Map<String, String[]> getParameterMap() {
		final HashMap<String, String[]> ret = HashMap.newHashMap(map.size());
		for (final Map.Entry<String, String[]> entry : map.entrySet()) {
			ret.put(entry.getKey(), entry.getValue().clone());
		}
		return ret;
	}

	public static Parameters parseQueryOnly(final String query) {
		final HashMap<String, List<String>> parameters = new HashMap<>();

		if (query != null) {
			final Map<String, List<String>> queryParameters = UrlEncoding.parse(query);
			for (final Map.Entry<String, List<String>> ent : queryParameters.entrySet()) {
				parameters.computeIfAbsent(ent.getKey(), _ -> new ArrayList<>()).addAll(ent.getValue());
			}
		}

		return new Parameters(parameters);
	}

	public static Parameters parseUrlEncoded(final String query, final Reader body) {
		final HashMap<String, List<String>> parameters = new HashMap<>();

		if (query != null) {
			final Map<String, List<String>> queryParameters = UrlEncoding.parse(query);
			for (final Map.Entry<String, List<String>> ent : queryParameters.entrySet()) {
				parameters.computeIfAbsent(ent.getKey(), _ -> new ArrayList<>()).addAll(ent.getValue());
			}
		}

		try {
			final String content = readAll(body);
			final Map<String, List<String>> contentParams = UrlEncoding.parse(content);
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
