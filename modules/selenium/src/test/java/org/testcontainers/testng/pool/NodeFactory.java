package org.testcontainers.testng.pool;

import org.testcontainers.grid.containers.SeleniumNodeContainer;
import org.vibur.objectpool.PoolObjectFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory required by vibur-object-pool for creating / destroying selenium grid nodes.
 */
public class NodeFactory implements PoolObjectFactory<SeleniumNodeContainer> {

	private final List<SeleniumNodeContainer> nodes;
	private final AtomicInteger counter;

	public NodeFactory(final List<SeleniumNodeContainer> nodes) {
		if (nodes == null || nodes.size() < 1)
			throw new IllegalArgumentException("Collection can't be missing or empty");

		this.nodes = nodes;
		this.counter = new AtomicInteger(this.nodes.size() - 1);
	}

	@Override
	public SeleniumNodeContainer create() {
		return nodes.get(counter.getAndDecrement());
	}

	@Override
	public boolean readyToTake(final SeleniumNodeContainer obj) {
		return true;
	}

	@Override
	public boolean readyToRestore(final SeleniumNodeContainer obj) {
		return true;
	}

	@Override
	public void destroy(final SeleniumNodeContainer obj) {
		obj.stop();
		nodes.remove(obj);
	}
}