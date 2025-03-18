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

    private static final int LDAPS_PORT = 6360;

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

    public LLdapContainer withBaseDn(String baseDn) {
        withEnv("LLDAP_LDAP_BASE_DN", baseDn);
        return this;
    }

    public LLdapContainer withUserPass(String userPass) {
        withEnv("LLDAP_LDAP_USER_PASS", userPass);
        return this;
    }

    public int getLdapPort() {
        int port = getEnvMap().getOrDefault("LLDAP_LDAPS_OPTIONS__ENABLED", "false").equals("true")
            ? LDAPS_PORT
            : LDAP_PORT;
        return getMappedPort(port);
    }

    public String getLdapUrl() {
        String protocol = getEnvMap().getOrDefault("LLDAP_LDAPS_OPTIONS__ENABLED", "false").equals("true")
            ? "ldaps"
            : "ldap";
        return String.format("%s://%s:%d", protocol, getHost(), getLdapPort());
    }

    public String getBaseDn() {
        return getEnvMap().getOrDefault("LLDAP_LDAP_BASE_DN", "dc=example,dc=com");
    }

    public String getUser() {
        return String.format("cn=admin,ou=people,%s", getBaseDn());
    }

    @Deprecated
    public String getUserPass() {
        return getEnvMap().getOrDefault("LLDAP_LDAP_USER_PASS", "password");
    }

    public String getPassword() {
        return getEnvMap().getOrDefault("LLDAP_LDAP_USER_PASS", "password");
    }
}
