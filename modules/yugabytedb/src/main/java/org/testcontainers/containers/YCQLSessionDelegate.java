package org.testcontainers.containers;

import com.datastax.oss.driver.api.core.CqlSession;

/**
 * A builder abstraction to construct {@link CqlSession} instance from
 * {@link YugabyteDBYCQLContainer} instance.
 *
 * @author srinivasa-vasu
 */
public interface YCQLSessionDelegate {

	/**
	 * Constructs a {@link CqlSession} instance from {@link YugabyteDBYCQLContainer}
	 * instance.
	 * @param container YCQL container instance
	 * @return {@link CqlSession} instance
	 */
	default CqlSession builder(YugabyteDBYCQLContainer container) {
		return CqlSession.builder().withLocalDatacenter(container.getLocalDc()).withKeyspace(container.getKeyspace())
				.withAuthCredentials(container.getUsername(), container.getPassword())
				.addContactPoint(container.getContactPoint()).build();
	}

}
