package org.testcontainers.containers.traits;

import com.github.dockerjava.api.command.CreateContainerCmd;
import org.testcontainers.containers.Container;

public interface Trait<T extends Container<T>> {

    void configure(T container, CreateContainerCmd createContainerCmd);

}
