package org.testcontainers.containers.wait.experimental;

import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class ComplexWaitStrategy extends AbstractWaitStrategy {

    private final List<WaitStrategy> waitStrategyList = new ArrayList<>();

    @Override
    protected void waitUntilReady() {
        ExecutorService service = Executors.newFixedThreadPool(waitStrategyList.size());
        List<? extends Future<?>> futures = waitStrategyList.stream()
            .map(waitStrategy -> service.submit(() -> waitStrategy.waitUntilReady(waitStrategyTarget)))
            .collect(Collectors.toList());

        Unreliables.retryUntilTrue(
            (int) startupTimeout.getSeconds(),
            TimeUnit.SECONDS,
            () -> futures.stream().anyMatch(Future::isDone)
        );
    }

    public ComplexWaitStrategy with(WaitStrategy waitStrategy) {
        this.waitStrategyList.add(waitStrategy);
        return this;
    }
}
