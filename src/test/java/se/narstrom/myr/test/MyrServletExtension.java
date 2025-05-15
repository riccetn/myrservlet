package se.narstrom.myr.test;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.core.spi.LoadableExtension;

public final class MyrServletExtension implements LoadableExtension {

	@Override
	public void register(final ExtensionBuilder builder) {
		builder.service(DeployableContainer.class, MyrServletDeployableContainer.class);
	}

}
