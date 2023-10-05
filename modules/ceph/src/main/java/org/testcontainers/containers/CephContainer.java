package org.testcontainers.containers;

import lombok.Getter;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Testcontainers implementation for Ceph.
 * <p>
 * Supported image: {@code quay.io/ceph/demo}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Ceph: 8080</li>
 *     <li>Monitor: 3300</li>
 * </ul>
 */
@Getter
public class CephContainer extends GenericContainer<CephContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("quay.io/ceph/demo");

    private static final Integer CEPH_MON_DEFAULT_PORT = 3300;

    private static final Integer CEPH_RGW_DEFAULT_PORT = 8080;

    private static final String CEPH_DEMO_UID = "admin";

    private static final String CEPH_END_START = ".*/opt/ceph-container/bin/demo: SUCCESS.*";

    private static final Set<String> CEPH_DEMO_DAEMONS = new HashSet<>(Collections.singletonList("all"));

    private String cephAccessKey;

    private String cephSecretKey;

    public CephContainer(final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public CephContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    @Override
    public void configure() {
        addExposedPorts(CEPH_MON_DEFAULT_PORT, CEPH_RGW_DEFAULT_PORT);

        addEnv("DEMO_DAEMONS", String.join(",", CEPH_DEMO_DAEMONS));
        addEnv("CEPH_DEMO_UID", CEPH_DEMO_UID);
        addEnv(
            "CEPH_DEMO_ACCESS_KEY",
            this.cephAccessKey != null
                ? this.cephAccessKey
                : (this.cephAccessKey = RandomStringUtils.randomAlphanumeric(32))
        );
        addEnv(
            "CEPH_DEMO_SECRET_KEY",
            this.cephSecretKey != null
                ? this.cephSecretKey
                : (this.cephSecretKey = RandomStringUtils.randomAlphanumeric(32))
        );
        addEnv("NETWORK_AUTO_DETECT", "1");
        addEnv("CEPH_DAEMON", "DEMO");
        addEnv("CEPH_PUBLIC_NETWORK", "0.0.0.0/0");
        addEnv("MON_IP", "127.0.0.1");
        addEnv("RGW_NAME", "localhost");

        setWaitStrategy(Wait.forLogMessage(CEPH_END_START, 1).withStartupTimeout(Duration.ofMinutes(5)));
    }

    public CephContainer withCephAccessKey(String cephAccessKey) {
        this.cephAccessKey = cephAccessKey;
        return this;
    }

    public CephContainer withCephSecretKey(String cephSecretKey) {
        this.cephSecretKey = cephSecretKey;
        return this;
    }

    public int getCephPort() {
        return getMappedPort(8080);
    }

    public URI getCephUrl() throws URISyntaxException {
        return new URI(String.format("http://%s:%s", this.getHost(), getCephPort()));
    }
}
