package se.narstrom.test.servlet.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import se.narstrom.test.servlet.http.semantics.Method;
import se.narstrom.test.servlet.http.semantics.Token;
import se.narstrom.test.servlet.http.v1.AbsolutePath;
import se.narstrom.test.servlet.uri.Query;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MyrServletRequest implements HttpServletRequest
{
   private final Method method;

   private final AbsolutePath path;

   private final Query query;

   private final Map<Token, List<String>> fields;

   private final Socket socket;

   private final InputStream inputStream;

   private final Map<String, Object> attributes = new HashMap<>();

   private Map<String, List<String>> parameters = null;

   public MyrServletRequest(final Method method, final AbsolutePath path, final Query query, final Map<Token, List<String>> fields, final Socket socket,
            final InputStream inputStream)
   {
      this.method = method;
      this.path = path;
      this.query = query;
      this.fields = fields;
      this.socket = socket;
      this.inputStream = inputStream;
   }

   @Override
   public Object getAttribute(String name)
   {
      return attributes.get(name);
   }

   @Override
   public Enumeration<String> getAttributeNames()
   {
      return Collections.enumeration(attributes.keySet());
   }

   @Override
   public String getCharacterEncoding()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setCharacterEncoding(String env) throws UnsupportedEncodingException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public int getContentLength()
   {
      return getIntHeader("content-length");
   }

   @Override
   public long getContentLengthLong()
   {
      return Long.parseLong(getHeader("content-length"));
   }

   @Override
   public String getContentType()
   {
      return getHeader("content-type");
   }

   @Override
   public ServletInputStream getInputStream() throws IOException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public String getParameter(String name)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Enumeration<String> getParameterNames()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public String[] getParameterValues(String name)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Map<String, String[]> getParameterMap()
   {
      maybeInitParameters();
      return parameters.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toArray(String[]::new)));
   }

   private void maybeInitParameters()
   {
      if (parameters != null)
         return;

      parameters = new HashMap<>();

      if (!query.value().isEmpty())
      {
         final String[] params = query.value().split("&", -1);
         for (final String param : params)
         {
            final int equals = param.indexOf('=');
            final String name = equals < 0 ? param : param.substring(0, equals);
            final String value = equals < 0 ? "" : param.substring(equals + 1);
            parameters.computeIfAbsent(name, __ -> new ArrayList<>()).add(value);
         }
      }

      if (getContentType().startsWith("application/x-www-urlencoded"))
      {
         
      }
   }

   @Override
   public String getProtocol()
   {
      return "HTTP/1.1";
   }

   @Override
   public String getScheme()
   {
      return "http";
   }

   @Override
   public String getServerName()
   {
      return "Test Server";
   }

   @Override
   public int getServerPort()
   {
      return socket.getLocalPort();
   }

   @Override
   public BufferedReader getReader() throws IOException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public String getRemoteAddr()
   {
      return socket.getInetAddress().getHostAddress();
   }

   @Override
   public String getRemoteHost()
   {
      return socket.getInetAddress().getHostName();
   }

   @Override
   public void setAttribute(final String name, final Object obj)
   {
      if (obj == null)
         attributes.remove(name);
      else
         attributes.put(name, obj);
   }

   @Override
   public void removeAttribute(final String name)
   {
      attributes.remove(name);
   }

   @Override
   public Locale getLocale()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Enumeration<Locale> getLocales()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isSecure()
   {
      return false;
   }

   @Override
   public RequestDispatcher getRequestDispatcher(String path)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public int getRemotePort()
   {
      return socket.getPort();
   }

   @Override
   public String getLocalName()
   {
      return socket.getLocalAddress().getHostName();
   }

   @Override
   public String getLocalAddr()
   {
      return socket.getLocalAddress().getHostAddress();
   }

   @Override
   public int getLocalPort()
   {
      return socket.getLocalPort();
   }

   @Override
   public ServletContext getServletContext()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public AsyncContext startAsync() throws IllegalStateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isAsyncStarted()
   {
      return false;
   }

   @Override
   public boolean isAsyncSupported()
   {
      return false;
   }

   @Override
   public AsyncContext getAsyncContext()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public DispatcherType getDispatcherType()
   {
      return DispatcherType.REQUEST;
   }

   @Override
   public String getRequestId()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public String getProtocolRequestId()
   {
      return "";
   }

   @Override
   public ServletConnection getServletConnection()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public String getAuthType()
   {
      return null;
   }

   @Override
   public Cookie[] getCookies()
   {
      return new Cookie[0];
   }

   @Override
   public long getDateHeader(final String name)
   {
      return LocalDateTime.parse(getHeader(name), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
   }

   @Override
   public String getHeader(final String name)
   {
      return fields.get(new Token(name)).getFirst();
   }

   @Override
   public Enumeration<String> getHeaders(String name)
   {
      return Collections.enumeration(fields.get(new Token(name)));
   }

   @Override
   public Enumeration<String> getHeaderNames()
   {
      return Collections.enumeration(fields.keySet().stream().map(Token::value).toList());
   }

   @Override
   public int getIntHeader(final String name)
   {
      return Integer.parseInt(getHeader(name));
   }

   @Override
   public String getMethod()
   {
      return method.token().value();
   }

   @Override
   public String getPathInfo()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public String getPathTranslated()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public String getContextPath()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public String getQueryString()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public String getRemoteUser()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isUserInRole(String role)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Principal getUserPrincipal()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public String getRequestedSessionId()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public String getRequestURI()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public StringBuffer getRequestURL()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public String getServletPath()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public HttpSession getSession(boolean create)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public HttpSession getSession()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public String changeSessionId()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isRequestedSessionIdValid()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isRequestedSessionIdFromCookie()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isRequestedSessionIdFromURL()
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean authenticate(final HttpServletResponse response) throws IOException, ServletException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public void login(String username, String password) throws ServletException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public void logout() throws ServletException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Collection<Part> getParts() throws IOException, ServletException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public Part getPart(String name) throws IOException, ServletException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException
   {
      throw new UnsupportedOperationException();
   }
}
