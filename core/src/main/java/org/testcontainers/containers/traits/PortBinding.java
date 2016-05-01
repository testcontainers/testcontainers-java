package org.testcontainers.containers.traits;

import com.github.dockerjava.api.command.CreateContainerCmd;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.Container;
import org.testcontainers.utility.SelfReference;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@RequiredArgsConstructor
public class PortBinding<SELF extends Container<SELF>> implements Trait<SELF> {

    public interface Support<SELF extends Container<SELF>> extends SelfReference<SELF> {
        default List<String> getPortBindings() {
            Stream<PortBinding> traits = self().getTraits(PortBinding.class);
            return traits.map(PortBinding::getBinding).collect(Collectors.toList());
        }

        default void setPortBindings(List<String> newValues) {
            self().replaceTraits(PortBinding.class, newValues.stream().map(PortBinding::new));
        }
    }

    protected final String binding;

    public PortBinding(int hostPort, int containerPort) {
        this(String.format("%d:%d", hostPort, containerPort));
    }


    @Override
    public void configure(SELF container, CreateContainerCmd createContainerCmd) {
        Stream<PortBinding> traits = container.getTraits(PortBinding.class);
        createContainerCmd.withPortBindings(
                traits.map(PortBinding::getBinding)
                        .map(com.github.dockerjava.api.model.PortBinding::parse)
                        .toArray(com.github.dockerjava.api.model.PortBinding[]::new)
        );
    }
}
