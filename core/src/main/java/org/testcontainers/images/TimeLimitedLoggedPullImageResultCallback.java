package org.testcontainers.images;

import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.slf4j.Logger;

import java.io.Closeable;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testcontainers.DockerClientFactory.TESTCONTAINERS_THREAD_GROUP;

/**
 * {@link PullImageResultCallback} with improved logging of pull progress and a 'watchdog' which will abort the pull
 * if progress is not being made, to prevent a hanging test
 */
public class TimeLimitedLoggedPullImageResultCallback extends LoggedPullImageResultCallback {

    private static final AtomicInteger THREAD_ID = new AtomicInteger(0);
    private static final ScheduledExecutorService PROGRESS_WATCHDOG_EXECUTOR =
        Executors.newScheduledThreadPool(0, runnable -> {
            Thread t = new Thread(TESTCONTAINERS_THREAD_GROUP, runnable);
            t.setDaemon(true);
            t.setName("testcontainers-pull-watchdog-" + THREAD_ID.incrementAndGet());
            return t;
        });
    private static final Duration PULL_PAUSE_TOLERANCE = Duration.ofSeconds(30);
    private final Logger logger;

    private ScheduledFuture<?> nextCheckForProgress;
    private Set<Thread> waitingThreads = new HashSet<>();

    TimeLimitedLoggedPullImageResultCallback(Logger logger) {
        super(logger);
        this.logger = logger;
    }

    @Override
    public PullImageResultCallback awaitCompletion() throws InterruptedException {
        waitingThreads.add(Thread.currentThread());
        return super.awaitCompletion();
    }

    @Override
    public void onNext(PullResponseItem item) {
        if (item.getProgressDetail() != null) {
            resetProgressWatchdog(false);
        }
        super.onNext(item);
    }

    @Override
    public void onStart(Closeable stream) {
        resetProgressWatchdog(false);
        super.onStart(stream);
    }

    @Override
    public void onError(Throwable throwable) {
        resetProgressWatchdog(true);
        super.onError(throwable);
    }

    @Override
    public void onComplete() {
        resetProgressWatchdog(true);
        super.onComplete();
    }


    /*
     * This method schedules a future task which will interrupt the waiting waiting threads if ever fired.
     * Every time this method is called (from onStart or onNext), the task is cancelled and recreated 30s in the future,
     * ensuring that it will only fire if the method stops being called regularly (e.g. if the pull has hung).
     */
    private void resetProgressWatchdog(boolean isFinished) {
        if (nextCheckForProgress != null && ! nextCheckForProgress.isCancelled()) {
            nextCheckForProgress.cancel(true);
        }
        if (!isFinished) {
            nextCheckForProgress = PROGRESS_WATCHDOG_EXECUTOR.schedule(() -> {
                logger.error("Docker image pull has not made progress in {}s - aborting pull", PULL_PAUSE_TOLERANCE.getSeconds());
                waitingThreads.forEach(Thread::interrupt);
            }, PULL_PAUSE_TOLERANCE.getSeconds(), TimeUnit.SECONDS);
        }
    }
}
