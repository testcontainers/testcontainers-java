package org.testcontainers.containers;

import com.github.dockerjava.api.command.CreateNetworkCmd;
import lombok.*;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;
import org.junit.rules.ExternalResource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.ResourceReaper;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public interface Network extends AutoCloseable {

    String getName();

    Boolean getEnableIpv6();

    String getDriver();

    boolean isCreated();

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
    @FieldDefaults(level = AccessLevel.PRIVATE)
    class NetworkImpl implements Network {

        final String name = UUID.randomUUID().toString();

        Boolean enableIpv6;

        String driver;

        @Singular
        Set<Consumer<CreateNetworkCmd>> createNetworkCmdModifiers = new LinkedHashSet<>();

        @Override
        public boolean isCreated() {
            return false;
        }
    }

    @RequiredArgsConstructor
    class Runnable implements Network, java.lang.Runnable {

        @Delegate(excludes = Excludes.class)
        protected final Network network;

        private final AtomicBoolean created = new AtomicBoolean(false);

        @Override
        public void run() {
            if (!created.getAndSet(true)) {
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

                if (network instanceof NetworkImpl) {
                    for (Consumer<CreateNetworkCmd> consumer : ((NetworkImpl) network).getCreateNetworkCmdModifiers()) {
                        consumer.accept(createNetworkCmd);
                    }
                }

                createNetworkCmd.exec();
            }
        }

        @Override
        public boolean isCreated() {
            return created.get();
        }
    }

    class AutoCreated implements Network {

        @Delegate(excludes = Excludes.class)
        protected final Network network;

        public AutoCreated(Network network) {
            this.network = network;
            as(Runnable.class).run();
        }

        @Override
        public boolean isCreated() {
            return true;
        }
    }

    class JUnitRule extends ExternalResource implements Network {

        @Delegate(types = Network.class)
        protected final Network.Runnable network;

        public JUnitRule(Network network) {
            this.network = network.as(Runnable.class);
        }

        @Override
        protected void before() throws Throwable {
            network.run();
        }

        @Override
        protected void after() {
            network.close();
        }
    }

    interface Excludes {
        boolean isCreated();
    }
}
