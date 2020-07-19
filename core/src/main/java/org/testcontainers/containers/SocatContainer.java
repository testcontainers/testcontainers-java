package org.testcontainers.containers;

import org.apache.commons.lang.StringUtils;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A socat container is used as a TCP proxy, enabling any TCP port of another container to be exposed
 * publicly, even if that container does not make the port public itself.
 */
public class SocatContainer extends GenericContainer<SocatContainer> {

    private static final String SOCAT_COMMAND_FORMAT = "socat %s:%d,fork,reuseaddr %s:%s";

    private final Map<InternetProtocol, Map<Integer, String>> targets;

    public SocatContainer() {
        super(TestcontainersConfiguration.getInstance().getSocatContainerImage());
        withCreateContainerCmdModifier(it -> it.withEntrypoint("/bin/sh"));
        withCreateContainerCmdModifier(it -> it.withName("testcontainers-socat-" + Base58.randomString(8)));
        this.targets = getInitialTargets();
    }

    private Map<InternetProtocol, Map<Integer, String>> getInitialTargets() {
        Map<InternetProtocol, Map<Integer, String>> targets = new EnumMap<>(InternetProtocol.class);
        targets.put(InternetProtocol.TCP, new HashMap<>());
        targets.put(InternetProtocol.UDP, new HashMap<>());
        return Collections.unmodifiableMap(targets);
    }

    public SocatContainer withTarget(int exposedPort, String host) {
        return withTarget(exposedPort, host, exposedPort);
    }

    public SocatContainer withTarget(int exposedPort, String host, int internalPort) {
        return withTarget(exposedPort, host, internalPort, InternetProtocol.TCP);
    }

    public SocatContainer withTarget(int exposedPort, String host, int internalPort, InternetProtocol internetProtocol) {
        if(isProtocolNotSupported(internetProtocol)) {
            throw new IllegalArgumentException("Internet protocol " + internetProtocol + " is not supported by Socat container");
        }
        targets.get(internetProtocol)
            .put(exposedPort, String.format("%s:%s", host, internalPort));
        addExposedPort(exposedPort, internetProtocol);
        return self();
    }

    private boolean isProtocolNotSupported(InternetProtocol internetProtocol) {
        return !targets.containsKey(internetProtocol);
    }

    @Override
    protected void configure() {
        withCommand("-c", socatCommand());
    }

    private String socatCommand() {
        String socatTcpCommand = createCommandForProtocol(InternetProtocol.TCP);
        String socatUdpCommand = createCommandForProtocol(InternetProtocol.UDP);

        return getFinalCommand(socatTcpCommand, socatUdpCommand);
    }

    private String getFinalCommand(String... commands) {
        return Arrays.stream(commands)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(" & "));
    }

    private String createCommandForProtocol(InternetProtocol internetProtocol) {
        SocatCmdProtocol socatCmdProtocol = SocatCmdProtocol.fromInternetProtocol(internetProtocol);
        return targets.get(internetProtocol)
            .entrySet()
            .stream()
            .map(entry -> String.format(SOCAT_COMMAND_FORMAT, socatCmdProtocol.inboundParameter, entry.getKey(), socatCmdProtocol.outboundParameter, entry.getValue()))
            .collect(Collectors.joining(" & "));
    }

    private enum SocatCmdProtocol {
        TCP("TCP-LISTEN", "TCP"),
        UDP("UDP4-RECVFROM", "UDP4-SENDTO");

        private final String inboundParameter;
        private final String outboundParameter;

        SocatCmdProtocol(String inboundParameter, String outboundParameter) {
            this.inboundParameter = inboundParameter;
            this.outboundParameter = outboundParameter;
        }

        private static SocatCmdProtocol fromInternetProtocol(InternetProtocol internetProtocol) {
            return Arrays.stream(values())
                .filter(socatCmdProtocol -> socatCmdProtocol.toString().equalsIgnoreCase(internetProtocol.toDockerNotation()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("There is no support for " + internetProtocol.toDockerNotation() + "in socat container"));
        }
    }
}
