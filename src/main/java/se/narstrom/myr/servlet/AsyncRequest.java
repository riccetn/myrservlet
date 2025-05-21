package se.narstrom.myr.servlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import se.narstrom.myr.http.v1.AbsolutePath;
import se.narstrom.myr.http.v1.RequestTarget;
import se.narstrom.myr.uri.Query;
import se.narstrom.myr.uri.UrlEncoding;

public final class AsyncRequest extends HttpServletRequestWrapper {
	private final Context context;

	private final AbsolutePath path;

	private final Query query;

	private Map<String, List<String>> parameters;

	public AsyncRequest(final HttpServletRequest request, final Context context, final String path) {
		super(request);
		this.context = context;

		final RequestTarget target = RequestTarget.parse(path);
		this.path = target.absolutePath();
		this.query = target.query();
	}

	@Override
	public String getRequestURI() {
		return getContextPath() + path.toString();
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.ASYNC;
	}

	@Override
	public ServletContext getServletContext() {
		return context;
	}

	@Override
	public String getContextPath() {
		return context.getContextPath();
	}

	@Override
	public String getQueryString() {
		return query.value();
	}

	@Override
	public String getParameter(final String name) {
		maybeInitParameters();

		final List<String> values = parameters.get(name);
		if (values != null)
			return values.getFirst();
		else
			return super.getParameter(name);
	}

	@Override
	public Enumeration<String> getParameterNames() {
		maybeInitParameters();

		final List<String> names = new ArrayList<>();
		names.addAll(parameters.keySet());

		for (Enumeration<String> iter = super.getParameterNames(); iter.hasMoreElements();) {
			names.add(iter.nextElement());
		}

		return Collections.enumeration(names);
	}

	@Override
	public String[] getParameterValues(final String name) {
		maybeInitParameters();

		final List<String> values = parameters.get(name);
		if (values != null)
			return values.toArray(String[]::new);

		return super.getParameterValues(name);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		final HashMap<String, String[]> map = new HashMap<>();

		map.putAll(super.getParameterMap());

		for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
			map.put(entry.getKey(), entry.getValue().toArray(String[]::new));
		}

		return Collections.unmodifiableMap(map);
	}

	private void maybeInitParameters() {
		if (parameters != null)
			return;

		parameters = new HashMap<>();

		if (!query.value().isEmpty()) {
			final Map<String, List<String>> queryParameters = UrlEncoding.parse(query.value());
			for (final Map.Entry<String, List<String>> ent : queryParameters.entrySet()) {
				parameters.computeIfAbsent(ent.getKey(), _ -> new ArrayList<>()).addAll(ent.getValue());
			}
		}
	}
}
