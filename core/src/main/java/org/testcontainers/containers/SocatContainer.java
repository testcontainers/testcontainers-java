package org.testcontainers.containers;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

/**
 * A socat container is used as a TCP proxy, enabling any TCP port of another container to be exposed
 * publicly, even if that container does not make the port public itself.
 */
public class SocatContainer extends GenericContainer<SocatContainer> {

    private final Map<Integer, String> targets = new HashMap<>();

    public SocatContainer() {
        this(DockerImageName.parse("alpine/socat:1.7.3.4-r0"));
    }

    public SocatContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        withCreateContainerCmdModifier(it -> it.withEntrypoint("/bin/sh"));
        withCreateContainerCmdModifier(it -> it.withName("testcontainers-socat-" + Base58.randomString(8)));
    }

    public SocatContainer withTarget(int exposedPort, String host) {
        return withTarget(exposedPort, host, exposedPort);
    }

    public SocatContainer withTarget(int exposedPort, String host, int internalPort) {
        addExposedPort(exposedPort);
        targets.put(exposedPort, String.format("%s:%s", host, internalPort));
        return self();
    }

    @Override
    protected void configure() {
        withCommand("-c",
                targets.entrySet().stream()
                        .map(entry -> "socat TCP-LISTEN:" + entry.getKey() + ",fork,reuseaddr TCP:" + entry.getValue())
                        .collect(Collectors.joining(" & "))
        );
    }
}
