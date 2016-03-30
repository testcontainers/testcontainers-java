package org.testcontainers.testng.pool;

import org.testcontainers.grid.enums.Browser;
import org.testcontainers.grid.containers.GenericGridContainer;
import org.testcontainers.grid.containers.SeleniumHubContainer;
import org.testcontainers.grid.containers.SeleniumNodeContainer;
import org.vibur.objectpool.ConcurrentLinkedPool;
import org.vibur.objectpool.PoolService;

import java.io.File;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.testcontainers.grid.enums.SeleniumImage.extractImage;

/**
 * Wrapped vibur-object-pool initializer; requires hub instance, browser type and pool size specification.
 */
public final class PoolFactory {

	public static PoolService<SeleniumNodeContainer> getNodePool(final SeleniumHubContainer hub, final Browser browser, final int poolSize) {
		return new ConcurrentLinkedPool<>(getNodesFactory(hub, browser, poolSize), 1, poolSize, true, new NodeListener());
	}

	private static NodeFactory getNodesFactory(final SeleniumHubContainer hub, final Browser browser, final int poolSize) {
		return new NodeFactory(IntStream.range(0, poolSize)
				.parallel()
				.mapToObj(i -> new SeleniumNodeContainer(extractImage(browser))
						.withHubAddress(hub.getHubAddress())
						.withHubPort(hub.getHubPort())
						.withRecordingMode(GenericGridContainer.VncRecordingMode.RECORD_ALL, new File("target"))
						.withLinkToContainer(hub, "hub"))
				.collect(toList()));
	}

	private PoolFactory() {
		throw new UnsupportedOperationException("Illegal access to private constructor");
	}
}
