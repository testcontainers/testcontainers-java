package org.testcontainers.containers.traits;

import com.github.dockerjava.api.command.CreateContainerCmd;
import org.testcontainers.containers.TestContainer;

public interface Trait<T extends TestContainer<T>> {

    void configure(T container, CreateContainerCmd createContainerCmd);

}
