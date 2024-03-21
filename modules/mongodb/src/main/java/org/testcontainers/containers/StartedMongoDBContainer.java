package org.testcontainers.containers;

public interface StartedMongoDBContainer extends StartedContainer {
    String getConnectionString();
}
