package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

/**
 * @author srinivasa-vasu
 */
public interface YugabyteTestContainerConstants {

	String IMAGE_NAME = "yugabytedb/yugabyte:2.7.2.0-b216";

	DockerImageName YBDB_TEST_IMAGE = DockerImageName.parse(IMAGE_NAME);

	String LOCAL_DC = "datacenter1";

	int YCQL_PORT = 9042;

}
