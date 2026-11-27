package se.narstrom.myr.test;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

public final class MyrServletContainerConfiguration implements ContainerConfiguration {

	/** Port the servlet container listens for HTTP requests on. */
	private int httpPort = 8080;

	/** Extra JVM arguments for the server JVM, separated by whitespace. */
	private String javaVmArguments = "";

	/** Milliseconds to wait for the server JVM to start or stop. */
	private int timeout = 60_000;

	public int getHttpPort() {
		return httpPort;
	}

	public void setHttpPort(final int httpPort) {
		this.httpPort = httpPort;
	}

	public String getJavaVmArguments() {
		return javaVmArguments;
	}

	public void setJavaVmArguments(final String javaVmArguments) {
		this.javaVmArguments = javaVmArguments;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(final int timeout) {
		this.timeout = timeout;
	}

	@Override
	public void validate() throws ConfigurationException {
		if (httpPort <= 0 || httpPort > 65535)
			throw new ConfigurationException("Invalid httpPort: " + httpPort);
		if (timeout <= 0)
			throw new ConfigurationException("Invalid timeout: " + timeout);
	}

}
