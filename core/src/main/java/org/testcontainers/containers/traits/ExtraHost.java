package org.testcontainers.containers.traits;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.google.common.collect.ObjectArrays;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.Container;
import org.testcontainers.utility.SelfReference;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@RequiredArgsConstructor
public class ExtraHost<SELF extends Container<SELF>> implements Trait<SELF> {

    public interface Support<SELF extends Container<SELF>> extends SelfReference<SELF> {

        /**
         * Add an extra host entry to be passed to the container
         * @param hostname
         * @param ipAddress
         * @return this
         */
        default SELF withExtraHost(String hostname, String ipAddress) {
            return self().with(new ExtraHost<>(hostname, ipAddress));
        }

        default List<String> getExtraHosts() {
            Stream<ExtraHost> traits = self().getTraits(ExtraHost.class);
            return traits.map(ExtraHost::getValue).collect(Collectors.toList());
        }

        default void setExtraHosts(List<String> extraHosts) {
            self().replaceTraits(ExtraHost.class, extraHosts.stream().map(ExtraHost::new));
        }
    }

    protected final String value;

    public ExtraHost(String hostname, String ipAddress) {
        this(String.format("%s:%s", hostname, ipAddress));
    }

    @Override
    public void configure(SELF container, CreateContainerCmd createContainerCmd) {
        String[] extraHosts = createContainerCmd.getExtraHosts();

        if (extraHosts == null) {
            extraHosts = new String[] {};
        }

        createContainerCmd.withExtraHosts(ObjectArrays.concat(extraHosts, value));
    }
}
