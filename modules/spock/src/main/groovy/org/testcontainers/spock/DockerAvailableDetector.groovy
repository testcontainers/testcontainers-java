package org.testcontainers.spock

import groovy.transform.CompileStatic
import org.testcontainers.DockerClientFactory

@CompileStatic
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
