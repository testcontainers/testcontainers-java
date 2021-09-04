package org.testcontainers.controller.intents;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface CreateNetworkIntent {
    CreateNetworkIntent withName(String name);

    CreateNetworkIntent withCheckDuplicate(boolean checkDuplicate);

    CreateNetworkIntent withEnableIpv6(Boolean enabledIpv6);

    CreateNetworkIntent withDriver(String driver);

    @Nullable
    Map<String, String> getLabels();

    CreateNetworkIntent withLabels(Map<String, String> labels);

    CreateNetworkResult perform();
}
