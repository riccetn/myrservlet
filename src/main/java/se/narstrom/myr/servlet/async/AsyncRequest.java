package se.narstrom.myr.servlet.async;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import se.narstrom.myr.servlet.Dispatcher;
import se.narstrom.myr.servlet.Parameters;
import se.narstrom.myr.uri.Query;

public final class AsyncRequest extends HttpServletRequestWrapper {
	private final Dispatcher dispatcher;

	private final Query query;

	private Parameters parameters;

	public AsyncRequest(final HttpServletRequest request, final Dispatcher dispatcher, final Query query) {
		super(request);
		this.dispatcher = dispatcher;
		this.query = query;
	}

	@Override
	public HttpServletMapping getHttpServletMapping() {
		return dispatcher.getMapping();
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.ASYNC;
	}

	@Override
	public String getParameter(final String name) {
		maybeInitParameters();

		String value = parameters.getParameter(name);
		if (value == null)
			value = super.getParameter(name);
		return value;
	}

	@Override
	public String[] getParameterValues(final String name) {
		maybeInitParameters();

		final List<String> values = new ArrayList<>();

		final String[] localValues = parameters.getParameterValues(name);
		if (localValues != null)
			values.addAll(Arrays.asList(localValues));

		final String[] superValues = super.getParameterValues(name);
		if (superValues != null)
			values.addAll(Arrays.asList(superValues));

		if (values.isEmpty())
			return null;

		return values.toArray(String[]::new);
	}

	@Override
	public Enumeration<String> getParameterNames() {
		maybeInitParameters();

		final Set<String> names = new HashSet<>();

		names.addAll(parameters.getParameterMap().keySet());
		names.addAll(super.getParameterMap().keySet());

		return Collections.enumeration(names);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		maybeInitParameters();

		final Map<String, String[]> params = new HashMap<>();
		params.putAll(parameters.getParameterMap());

		for (Map.Entry<String, String[]> entry : super.getParameterMap().entrySet()) {
			params.merge(entry.getKey(), entry.getValue(), (v1, v2) -> {
				final List<String> list = new ArrayList<>();
				list.addAll(Arrays.asList(v1));
				list.addAll(Arrays.asList(v2));
				return list.toArray(String[]::new);
			});
		}

		return Collections.unmodifiableMap(params);
	}

	private void maybeInitParameters() {
		if (parameters != null)
			return;

		parameters = Parameters.parseQueryOnly(query.toString());
	}
}
