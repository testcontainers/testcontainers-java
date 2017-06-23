package org.testcontainers.containers;

import com.github.dockerjava.api.command.CreateNetworkCmd;
import lombok.*;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.ResourceReaper;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Network extends AutoCloseable, TestRule {

    String getId();

    String getName();

    Boolean getEnableIpv6();

    String getDriver();

    boolean isCreated();

    default boolean create() {
        return getId() != null;
    }

    @Override
    default void close() {
        if (isCreated()) {
            ResourceReaper.instance().removeNetworks(getName());
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    default <T extends Network> T as(Class<T> type) {
        return type.getDeclaredConstructor(Network.class).newInstance(this);
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
        private final String id = ((Supplier<String>) () -> {
            ResourceReaper.instance().registerNetworkForCleanup(getName());

            CreateNetworkCmd createNetworkCmd = DockerClientFactory.instance().client().createNetworkCmd();

            createNetworkCmd.withName(getName());
            createNetworkCmd.withCheckDuplicate(true);

            if (getEnableIpv6() != null) {
                createNetworkCmd.withEnableIpv6(getEnableIpv6());
            }

            if (getDriver() != null) {
                createNetworkCmd.withDriver(getDriver());
            }

            for (Consumer<CreateNetworkCmd> consumer : createNetworkCmdModifiers) {
                consumer.accept(createNetworkCmd);
            }

            return createNetworkCmd.exec().getId();
        }).get();

        @Override
        public boolean isCreated() {
            // Lombok with @Getter(lazy = true) will use AtomicReference as a field type for id
            return ((AtomicReference<String>) (Object) id).get() != null;
        }

        @Override
        protected void before() throws Throwable {
            create();
        }

        @Override
        protected void after() {
            close();
        }
    }
}
