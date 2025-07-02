package se.narstrom.myr.servlet.deploy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;

import ee.jakarta.xml.ns.jakartaee.ErrorPageType;
import ee.jakarta.xml.ns.jakartaee.ListenerType;
import ee.jakarta.xml.ns.jakartaee.LocaleEncodingMappingListType;
import ee.jakarta.xml.ns.jakartaee.LocaleEncodingMappingType;
import ee.jakarta.xml.ns.jakartaee.MimeMappingType;
import ee.jakarta.xml.ns.jakartaee.ParamValueType;
import ee.jakarta.xml.ns.jakartaee.ServletMappingType;
import ee.jakarta.xml.ns.jakartaee.ServletType;
import ee.jakarta.xml.ns.jakartaee.TrueFalseType;
import ee.jakarta.xml.ns.jakartaee.UrlPatternType;
import ee.jakarta.xml.ns.jakartaee.WebAppType;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import jakarta.xml.bind.JAXB;
import se.narstrom.myr.servlet.DefaultServlet;
import se.narstrom.myr.servlet.context.Context;
import se.narstrom.myr.servlet.context.ServletClassLoader;
import se.narstrom.myr.servlet.session.SessionManager;

public final class Deployer {

	public static final Context deploy(final String contextPath, final Path base, final SessionManager sessionManager) throws IOException {
		final Path descriptor = base.resolve("WEB-INF/web.xml");
		final WebAppType webApp = JAXB.unmarshal(descriptor.toFile(), WebAppType.class);

		final Context context = new Context(contextPath, base, sessionManager);

		context.setServletContextName(webApp.getDisplayName().getFirst().getValue());

		for (final ParamValueType param : webApp.getContextParam()) {
			context.setInitParameter(param.getParamName().getValue(), param.getParamValue().getValue());
		}

		for (final ListenerType listener : webApp.getListener()) {
			context.addListener(listener.getListenerClass().getValue());
		}

		for (final ServletType servlet : webApp.getServlet()) {
			final String className = servlet.getServletClass().getValue();
			final String servletName = servlet.getServletName().getValue();

			final ServletRegistration.Dynamic registration = context.addServlet(servletName, className);

			for (final ParamValueType param : servlet.getInitParam()) {
				registration.setInitParameter(param.getParamName().getValue(), param.getParamValue().getValue());
			}

			final TrueFalseType asyncSupported = servlet.getAsyncSupported();
			if (asyncSupported != null)
				registration.setAsyncSupported(asyncSupported.isValue());
		}

		for (final ServletMappingType mapping : webApp.getServletMapping()) {
			final String servletName = mapping.getServletName().getValue();
			final ServletRegistration registration = context.getServletRegistration(servletName);
			for (final UrlPatternType pattern : mapping.getUrlPattern()) {
				registration.addMapping(pattern.getValue());
			}
		}

		for (final ErrorPageType errorPage : webApp.getErrorPage()) {
			if (errorPage.getErrorCode() != null)
				context.addErrorPage(errorPage.getErrorCode().getValue().intValue(), errorPage.getLocation().getValue());
			if (errorPage.getExceptionType() != null)
				context.addExceptionPage(errorPage.getExceptionType().getValue(), errorPage.getLocation().getValue());
		}

		final List<LocaleEncodingMappingListType> localeMappingLists = webApp.getLocaleEncodingMappingList();
		if (localeMappingLists.size() > 1)
			throw new IllegalArgumentException("Invalid deployment descriptor: contains more then one locale-encoding-mappig-list");
		if (localeMappingLists.size() == 1) {
			for (final LocaleEncodingMappingType mapping : webApp.getLocaleEncodingMappingList().getFirst().getLocaleEncodingMapping()) {
				context.addLocaleEncodingMapping(Locale.forLanguageTag(mapping.getLocale()), Charset.forName(mapping.getEncoding()));
			}
		}

		for (final MimeMappingType mapping : webApp.getMimeMapping()) {
			context.addMimeTypeMapping(mapping.getExtension().getValue(), mapping.getMimeType().getValue());
		}

		final ServletRegistration.Dynamic registration = context.addServlet("Default Servlet", new DefaultServlet());
		registration.addMapping("/");

		scanForAnnotations(context);

		return context;
	}

	private static void scanForAnnotations(final Context context) throws IOException {
		final ServletClassLoader classLoader = (ServletClassLoader) context.getClassLoader();
		final Path dir = Path.of(context.getRealPath("/"), "WEB-INF", "classes");

		if(!Files.isDirectory(dir))
			return;

		Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
				scanForAnnotations_visitClassFile(context, classLoader, dir.relativize(file));
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private static void scanForAnnotations_visitClassFile(final Context context, final ServletClassLoader classLoader, final Path file) throws IOException {
		final String pathString = file.toString();
		if (!pathString.endsWith(".class"))
			return;
		final String className = pathString.substring(0, pathString.length() - 6).replace(File.separatorChar, '.');
		final Class<?> clazz;
		try {
			clazz = classLoader.loadClass(className);
		} catch (final ClassNotFoundException ex) {
			throw new IOException(ex);
		}

		final WebServlet webServlet = clazz.getAnnotation(WebServlet.class);
		if (webServlet != null && Servlet.class.isAssignableFrom(clazz)) {
			final ServletRegistration.Dynamic registration = context.addServlet(webServlet.name(), (Class<? extends Servlet>) clazz);
			for (final WebInitParam param : webServlet.initParams()) {
				registration.setInitParameter(param.name(), param.value());
			}
			registration.addMapping(webServlet.urlPatterns());
			registration.addMapping(webServlet.value());
			registration.setAsyncSupported(webServlet.asyncSupported());
		}
	}
}
