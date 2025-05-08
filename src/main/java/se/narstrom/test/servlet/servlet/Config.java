package se.narstrom.test.servlet.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

import java.util.Collections;
import java.util.Enumeration;

public final class Config implements ServletConfig
{
   private final ServletContext context;

   public Config(final ServletContext context)
   {
      this.context = context;
   }

   @Override
   public String getServletName()
   {
      return "Default";
   }

   @Override
   public ServletContext getServletContext()
   {
      return context;
   }

   @Override
   public String getInitParameter(final String name)
   {
      return null;
   }

   @Override
   public Enumeration<String> getInitParameterNames()
   {
      return Collections.emptyEnumeration();
   }
}
