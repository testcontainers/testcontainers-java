package org.testcontainers.containers.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.YugabyteDBYCQLContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;

/**
 * Custom wait strategy for YCQL API.
 *
 * <p>
 * Though we can either use HTTP or PORT based wait strategy, when we create a custom
 * keyspace/role, it gets executed asynchronously. As the wait on container.start() on a
 * specific port wouldn't fully guarantee the custom object execution. It's better to
 * check the DB status with this way with a smoke test query that uses the underlying
 * custom objects and wait for the operation to complete.
 * </p>
 */
@RequiredArgsConstructor
@Slf4j
public final class YugabyteDBYCQLWaitStrategy extends AbstractWaitStrategy {

    private static final String YCQL_TEST_QUERY = "SELECT release_version FROM system.local";

    private static final String BIN_PATH = "/home/yugabyte/tserver/bin/ycqlsh";

    private final WaitStrategyTarget target;

    @Override
    public void waitUntilReady(WaitStrategyTarget target) {
        YugabyteDBYCQLContainer container = (YugabyteDBYCQLContainer) target;
        AtomicBoolean status = new AtomicBoolean(true);
        final String containerInterfaceIP = container
            .getContainerInfo()
            .getNetworkSettings()
            .getNetworks()
            .entrySet()
            .stream()
            .findFirst()
            .get()
            .getValue()
            .getIpAddress();
        retryUntilSuccess(
            (int) startupTimeout.getSeconds(),
            TimeUnit.SECONDS,
            () -> {
                YugabyteDBYCQLWaitStrategy.this.getRateLimiter()
                    .doWhenReady(() -> {
                        try {
                            ExecResult result = container.execInContainer(
                                BIN_PATH,
                                containerInterfaceIP,
                                "-u",
                                container.getUsername(),
                                "-p",
                                container.getPassword(),
                                "-k",
                                container.getKeyspace(),
                                "-e",
                                YCQL_TEST_QUERY
                            );
                            if (result.getExitCode() != 0) {
                                status.set(false);
                                log.debug(result.getStderr());
                            }
                        } catch (Exception e) {
                            status.set(false);
                            log.debug(e.getMessage(), e);
                        } finally {
                            if (!status.getAndSet(true)) {
                                throw new RuntimeException("container hasn't come up yet");
                            }
                        }
                    });
                return status;
            }
        );
    }

    @Override
    public void waitUntilReady() {
        waitUntilReady(target);
    }
}
