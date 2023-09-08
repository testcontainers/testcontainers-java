package org.testcontainers.core;

import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public final class DefaultTestcontainersConfiguration implements Configuration {

    private final TestcontainersConfiguration testcontainersConfiguration = TestcontainersConfiguration.getInstance();

    private static final Configuration INSTANCE = loadTestcontainersConfiguration();

    private static Configuration loadTestcontainersConfiguration() {
        List<Configuration> configurationList = new ArrayList<>();
        ServiceLoader<Configuration> configurations = ServiceLoader.load(Configuration.class);
        configurations.forEach(configurationList::add);
        List<Configuration> configurationsOrdered = configurationList
            .stream()
            .sorted(Comparator.comparing(Configuration::getPriority).reversed())
            .collect(Collectors.toList());
        return configurationsOrdered.isEmpty()
            ? new DefaultTestcontainersConfiguration()
            : configurationsOrdered.get(0);
    }

    private DefaultTestcontainersConfiguration() {}

    public static Configuration getInstance() {
        return INSTANCE;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public Duration getRyukTimeout() {
        return Duration.ofSeconds(this.testcontainersConfiguration.getRyukTimeout());
    }

    @Override
    public boolean environmentSupportsReuse() {
        return this.testcontainersConfiguration.environmentSupportsReuse();
    }

    @Override
    public String getDockerClientStrategyClassName() {
        return this.testcontainersConfiguration.getDockerClientStrategyClassName();
    }

    @Override
    public String getTransportType() {
        return this.testcontainersConfiguration.getTransportType();
    }

    @Override
    public Duration getImagePullPauseTimeout() {
        return Duration.ofSeconds(this.testcontainersConfiguration.getImagePullPauseTimeout());
    }

    @Override
    public String getImageSubstitutorClassName() {
        return this.testcontainersConfiguration.getImageSubstitutorClassName();
    }

    @Override
    public Duration getClientPingTimeout() {
        return Duration.ofSeconds(this.testcontainersConfiguration.getClientPingTimeout());
    }
}
