package org.testcontainers.containers.traits;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.google.common.collect.ObjectArrays;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.TestContainer;
import org.testcontainers.utility.SelfReference;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@RequiredArgsConstructor
public class ExposedPort<SELF extends TestContainer<SELF>> implements Trait<SELF> {

    public interface Support<SELF extends TestContainer<SELF>> extends SelfReference<SELF> {
        default void addExposedPort(Integer port) {
            self().with(new ExposedPort<>(port));
        }

        default void addExposedPorts(Integer... ports) {
            for (int port : ports) {
                addExposedPort(port);
            }
        }

        /**
         * Set the ports that this container listens on
         *
         * @param ports an array of TCP ports
         * @return this
         */
        default SELF withExposedPorts(Integer... ports) {
            addExposedPorts(ports);

            return self();
        }

        default List<Integer> getExposedPorts() {
            Stream<ExposedPort> traits = self().getTraits(ExposedPort.class);
            return traits.map(exposedPort -> exposedPort.getExposedPort().getPort()).collect(Collectors.toList());
        }

        default void setExposedPorts(List<Integer> exposedPorts) {
            self().replaceTraits(ExposedPort.class, exposedPorts.stream().map(ExposedPort::new));
        }
    }

    protected final com.github.dockerjava.api.model.ExposedPort exposedPort;

    public ExposedPort(int port) {
        this(new com.github.dockerjava.api.model.ExposedPort(port));
    }

    @Override
    public void configure(SELF container, CreateContainerCmd createContainerCmd) {
        com.github.dockerjava.api.model.ExposedPort[] currentExposedPorts = createContainerCmd.getExposedPorts();

        if (currentExposedPorts == null) {
            currentExposedPorts = new com.github.dockerjava.api.model.ExposedPort[] {};
        }

        // Set up exposed ports (where there are no host port bindings defined)
        createContainerCmd.withExposedPorts(ObjectArrays.concat(currentExposedPorts, exposedPort));
    }
}
