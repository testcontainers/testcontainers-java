package org.testcontainers.containers.wait.internal;

import java.util.concurrent.CompletableFuture;

public interface AsyncCheck {
    CompletableFuture<Boolean> perform();
}
