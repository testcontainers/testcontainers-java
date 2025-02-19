package org.testcontainers.ldap;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for LLDAP.
 * <p>
 * Supported image: {@code lldap/lldap}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>LDAP: 3890</li>
 *     <li>UI: 17170</li>
 * </ul>
 */
@Slf4j
public class LLdapContainer extends GenericContainer<LLdapContainer> {

    private static final String IMAGE_VERSION = "lldap/lldap";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(IMAGE_VERSION);

    private static final int LDAP_PORT = 3890;

    private static final int UI_PORT = 17170;

    public LLdapContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public LLdapContainer(DockerImageName image) {
        super(image);
        image.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        addExposedPorts(LDAP_PORT, UI_PORT);

        waitingFor(Wait.forHttp("/health").forPort(UI_PORT).forStatusCode(200));
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        log.info("LLDAP container is ready! UI available at http://{}:{}", getHost(), getMappedPort(UI_PORT));
    }

    public int getLdapPort() {
        return getMappedPort(LDAP_PORT);
    }
}
