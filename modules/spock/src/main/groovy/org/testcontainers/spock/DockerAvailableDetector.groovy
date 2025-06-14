package org.testcontainers.spock

import org.testcontainers.DockerClientFactory

class DockerAvailableDetector {

	boolean isDockerAvailable() {
		try {
			DockerClientFactory.instance().client();
			return true;
		} catch (Throwable ex) {
			return false;
		}
	}
}
