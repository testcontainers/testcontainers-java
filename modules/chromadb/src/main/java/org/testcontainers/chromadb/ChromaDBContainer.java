package org.testcontainers.chromadb;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation of ChromaDB.
 * <p>
 * Supported images: {@code chromadb/chroma}, {@code ghcr.io/chroma-core/chroma}
 * <p>
 * Exposed ports: 8000
 */
@Slf4j
public class ChromaDBContainer extends GenericContainer<ChromaDBContainer> {

    private static final DockerImageName DEFAULT_DOCKER_IMAGE = DockerImageName.parse("chromadb/chroma");

    private static final DockerImageName GHCR_DOCKER_IMAGE = DockerImageName.parse("ghcr.io/chroma-core/chroma");

    public ChromaDBContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public ChromaDBContainer(DockerImageName dockerImageName) {
        this(dockerImageName, isVersion2(dockerImageName.getVersionPart()));
    }

    public ChromaDBContainer(DockerImageName dockerImageName, boolean isVersion2) {
        super(dockerImageName);

        String apiPath = isVersion2 ? "/api/v2/heartbeat" : "/api/v1/heartbeat";
        dockerImageName.assertCompatibleWith(DEFAULT_DOCKER_IMAGE, GHCR_DOCKER_IMAGE);
        withExposedPorts(8000);
        waitingFor(Wait.forHttp(apiPath));
    }

    public String getEndpoint() {
        return "http://" + getHost() + ":" + getFirstMappedPort();
    }

    private static boolean isVersion2(String version) {
        if (version.equals("latest")) {
            return true;
        }

        ComparableVersion comparableVersion = new ComparableVersion(version);
        if (comparableVersion.isGreaterThanOrEqualTo("1.0.0")) {
            return true;
        }

        log.warn("Version {} is less than 1.0.0 or not a semantic version.", version);
        return false;
    }
}
