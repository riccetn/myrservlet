package se.narstrom.myr.servlet.filter;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import se.narstrom.myr.servlet.servlet.MyrServletRegistration;

public final class ExecutableFilterChain implements FilterChain {

	private final List<MyrFilterRegistration> filters;

	private final MyrServletRegistration servlet;

	private int index;

	public ExecutableFilterChain(final List<MyrFilterRegistration> filters, final MyrServletRegistration servlet) {
		this.filters = filters;
		this.servlet = servlet;
		this.index = -1;
	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response) throws IOException, ServletException {
		++index;
		try {
			if (index < filters.size()) {
				filters.get(index).service(request, response, this);
			} else {
				servlet.service(request, response);
			}
		} finally {
			--index;
		}
	}

	public MyrServletRegistration getServletRegistration() {
		return servlet;
	}
}
