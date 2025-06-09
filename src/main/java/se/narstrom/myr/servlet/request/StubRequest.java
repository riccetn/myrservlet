package se.narstrom.myr.servlet.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

public abstract class StubRequest implements HttpServletRequest {

	@Override
	public boolean authenticate(final HttpServletResponse response) throws IOException, ServletException {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String changeSessionId() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public AsyncContext getAsyncContext() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public Object getAttribute(String name) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getAuthType() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getCharacterEncoding() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public int getContentLength() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public long getContentLengthLong() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getContentType() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getContextPath() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public Cookie[] getCookies() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public long getDateHeader(final String name) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public DispatcherType getDispatcherType() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getHeader(final String name) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public Enumeration<String> getHeaders(final String name) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public int getIntHeader(String name) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getLocalAddr() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public Locale getLocale() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public Enumeration<Locale> getLocales() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getLocalName() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public int getLocalPort() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getMethod() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getParameter(final String name) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public Enumeration<String> getParameterNames() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String[] getParameterValues(final String name) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public Part getPart(final String name) throws IOException, ServletException {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getPathInfo() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getPathTranslated() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getProtocol() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getProtocolRequestId() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getQueryString() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public BufferedReader getReader() throws IOException {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getRemoteAddr() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getRemoteHost() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public int getRemotePort() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getRemoteUser() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public RequestDispatcher getRequestDispatcher(final String path) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getRequestedSessionId() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getRequestId() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getRequestURI() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public StringBuffer getRequestURL() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getScheme() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getServerName() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public int getServerPort() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public ServletConnection getServletConnection() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public ServletContext getServletContext() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public String getServletPath() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public HttpSession getSession() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public HttpSession getSession(final boolean create) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public Principal getUserPrincipal() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public boolean isAsyncStarted() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public boolean isAsyncSupported() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public boolean isSecure() {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public boolean isUserInRole(final String role) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void login(final String username, final String password) throws ServletException {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void logout() throws ServletException {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void removeAttribute(final String name) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void setAttribute(final String name, final Object o) {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public void setCharacterEncoding(final String encoding) throws UnsupportedEncodingException {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public AsyncContext startAsync(final ServletRequest servletRequest, final ServletResponse servletResponse) throws IllegalStateException {
		throw new UnsupportedOperationException("Stub");
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(final Class<T> handlerClass) throws IOException, ServletException {
		throw new UnsupportedOperationException("Stub");
	}

}
