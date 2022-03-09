package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

/**
 * @author srinivasa-vasu
 */
public interface YugabyteTestContainerConstants {

	String IMAGE_NAME = "yugabytedb/yugabyte:2.12.1.0-b41";

	DockerImageName YBDB_TEST_IMAGE = DockerImageName.parse(IMAGE_NAME);

	String LOCAL_DC = "datacenter1";

	int YCQL_PORT = 9042;

}
