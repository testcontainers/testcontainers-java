package org.testcontainers.testng.pool;

import org.testcontainers.grid.containers.SeleniumNodeContainer;
import org.vibur.objectpool.listener.Listener;

import java.util.logging.Logger;

/**
 * Author: Serhii Korol.
 */
public class NodeListener implements Listener<SeleniumNodeContainer> {

	private final Logger logger = Logger.getLogger(getClass().getName());

	@Override
	public void onTake(SeleniumNodeContainer object) {
		logger.info("Took " + object.getBrowser());
	}

	@Override
	public void onRestore(SeleniumNodeContainer object) {
		logger.info("Restored " + object.getBrowser());
	}
}
