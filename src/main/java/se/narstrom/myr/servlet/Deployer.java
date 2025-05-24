package se.narstrom.myr.servlet;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import ee.jakarta.xml.ns.jakartaee.ErrorPageType;
import ee.jakarta.xml.ns.jakartaee.LocaleEncodingMappingListType;
import ee.jakarta.xml.ns.jakartaee.LocaleEncodingMappingType;
import ee.jakarta.xml.ns.jakartaee.ParamValueType;
import ee.jakarta.xml.ns.jakartaee.ServletMappingType;
import ee.jakarta.xml.ns.jakartaee.ServletType;
import ee.jakarta.xml.ns.jakartaee.TrueFalseType;
import ee.jakarta.xml.ns.jakartaee.UrlPatternType;
import ee.jakarta.xml.ns.jakartaee.WebAppType;
import jakarta.servlet.ServletRegistration;
import jakarta.xml.bind.JAXB;

public final class Deployer {

	public static final Context deploy(final String contextPath, final Path base) throws IOException {
		final Path descriptor = base.resolve("WEB-INF/web.xml");
		final WebAppType webApp = JAXB.unmarshal(descriptor.toFile(), WebAppType.class);

		final Context context = new Context(contextPath, base);

		context.setServletContextName(webApp.getDisplayName().getFirst().getValue());

		for (final ParamValueType param : webApp.getContextParam()) {
			context.setInitParameter(param.getParamName().getValue(), param.getParamValue().getValue());
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

		final ServletRegistration.Dynamic registration = context.addServlet("Default Servlet", new DefaultServlet());
		registration.addMapping("/");

		return context;
	}
}
