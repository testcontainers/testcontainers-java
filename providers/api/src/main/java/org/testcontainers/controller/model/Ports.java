package org.testcontainers.controller.model;

import java.util.Map;

public interface Ports {
    Map<ExposedPort, Binding[]> getBindings(); // TODO: Rename to getAllBindings
    Binding[] getBindings(int port);
}
