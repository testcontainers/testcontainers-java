package org.testcontainers.containers;

import org.testcontainers.containers.JdbcDatabaseContainerProvider;

public class VirtuosoContainerProvider extends JdbcDatabaseContainerProvider {
	
	@Override
	public boolean supports(String databaseType) {
		return databaseType.equals(VirtuosoContainer.NAME);
	}

	@Override
	public VirtuosoContainer newInstance(String tag) {
		return new VirtuosoContainer(VirtuosoContainer.IMAGE + ":" + tag);
	}
	
}
