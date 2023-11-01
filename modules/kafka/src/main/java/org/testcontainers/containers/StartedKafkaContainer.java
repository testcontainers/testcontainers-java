package org.testcontainers.containers;

import org.testcontainers.UnstableAPI;

@UnstableAPI
interface StartedKafkaContainer extends StartedContainer {
    String getBootstrapServers();
}
