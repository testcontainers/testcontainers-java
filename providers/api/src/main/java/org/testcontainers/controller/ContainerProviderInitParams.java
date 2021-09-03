package org.testcontainers.controller;

import lombok.Data;
import org.testcontainers.controller.configuration.ConfigurationSource;

@Data
public class ContainerProviderInitParams {

    private final ConfigurationSource configurationSource;

    public ContainerProviderInitParams(ConfigurationSource configurationSource) {
        this.configurationSource = configurationSource;
    }

}
