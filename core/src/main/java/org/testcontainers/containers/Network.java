package org.testcontainers.containers;

import com.github.dockerjava.api.command.CreateNetworkCmd;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.ResourceReaper;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public interface Network extends AutoCloseable, TestRule {

    String getId();

    @Override
    default void close() {
        ResourceReaper.instance().removeNetworkById(getId());
    }

    static Network newNetwork() {
        return builder().build();
    }

    static NetworkImpl.NetworkImplBuilder builder() {
        return NetworkImpl.builder();
    }

    @Builder
    @Getter
    class NetworkImpl extends ExternalResource implements Network {

        private final String name = UUID.randomUUID().toString();

        private Boolean enableIpv6;

        private String driver;

        @Singular
        private Set<Consumer<CreateNetworkCmd>> createNetworkCmdModifiers = new LinkedHashSet<>();

        @Getter(lazy = true)
        private final String id = create();

        private String create() {
            CreateNetworkCmd createNetworkCmd = DockerClientFactory.instance().client().createNetworkCmd();

            createNetworkCmd.withName(name);
            createNetworkCmd.withCheckDuplicate(true);

            if (enableIpv6 != null) {
                createNetworkCmd.withEnableIpv6(enableIpv6);
            }

            if (driver != null) {
                createNetworkCmd.withDriver(driver);
            }

            for (Consumer<CreateNetworkCmd> consumer : createNetworkCmdModifiers) {
                consumer.accept(createNetworkCmd);
            }

            String id = createNetworkCmd.exec().getId();
            ResourceReaper.instance().registerNetworkIdForCleanup(id);
            return id;
        }

        @Override
        protected void after() {
            close();
        }
    }
}
