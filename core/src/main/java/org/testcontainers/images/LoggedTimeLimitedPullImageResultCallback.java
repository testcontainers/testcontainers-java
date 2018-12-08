package org.testcontainers.images;

import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.slf4j.Logger;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

/**
 * {@link PullImageResultCallback} with:
 * <ul>
 *     <li>Improved logging of pull progress</li>
 *     <li>A 'watchdog' which will abort the pull if progress is not being made, to prevent a hanging test</li>
 * </ul>
 */
public class LoggedTimeLimitedPullImageResultCallback extends PullImageResultCallback {
    private final Logger logger;

    private static final ScheduledExecutorService PROGRESS_WATCHDOG_EXECUTOR =
        Executors.newScheduledThreadPool(0, runnable -> {
            Thread t = new Thread(runnable);
            t.setDaemon(false);
            t.setName("testcontainers-pull-watchdog");
            return t;
        });
    private ScheduledFuture<?> nextCheckForProgress;

    private Set<String> allLayers = new HashSet<>();
    private Set<String> downloadedLayers = new HashSet<>();
    private Set<String> pulledLayers = new HashSet<>();
    private Map<String, Long> totalSizes = new HashMap<>();
    private Map<String, Long> currentSizes = new HashMap<>();
    private Thread thread;

    LoggedTimeLimitedPullImageResultCallback(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void onStart(final Closeable stream) {
        super.onStart(stream);
        resetProgressWatchdog(false);

        logger.info("Pulling image");
    }

    @Override
    public void onNext(final PullResponseItem item) {
        super.onNext(item);

        String status = item.getStatus();
        String id = item.getId();

        if (item.getProgressDetail() != null) {
            allLayers.add(id);
        }

        if (status != null && status.equalsIgnoreCase("Download complete")) {
            downloadedLayers.add(id);
        }

        if (status != null && status.equalsIgnoreCase("Pull complete")) {
            pulledLayers.add(id);
        }

        if (item.getProgressDetail() != null) {
            Long total = item.getProgressDetail().getTotal();
            Long current = item.getProgressDetail().getCurrent();

            if (total != null && total > totalSizes.getOrDefault(id, 0L)) {
                totalSizes.put(id, total);
            }
            if (current != null && current > currentSizes.getOrDefault(id, 0L)) {
                currentSizes.put(id, current);
            }

            resetProgressWatchdog(false);
        }

        if (status != null && (status.startsWith("Pulling from") || status.contains("complete"))) {

            long totalSize = totalLayerSize();
            long currentSize = downloadedLayerSize();

            int pendingCount = allLayers.size() - downloadedLayers.size();
            String friendlyTotalSize;
            if (pendingCount > 0) {
                friendlyTotalSize = "? MB";
            } else {
                friendlyTotalSize = byteCountToDisplaySize(totalSize);
            }

            logger.info("Pulling image layers: {} pending, {} downloaded, {} extracted, ({}/{})",
                format("%2d", pendingCount),
                format("%2d", downloadedLayers.size()),
                format("%2d", pulledLayers.size()),
                byteCountToDisplaySize(currentSize),
                friendlyTotalSize);
        }
    }

    @Override
    public void onComplete() {
        resetProgressWatchdog(true);
        super.onComplete();

        long totalSize = totalLayerSize();
        logger.info("Pull complete ({} layers, {})", allLayers.size(), byteCountToDisplaySize(totalSize));
    }

    @Override
    public PullImageResultCallback awaitCompletion() throws InterruptedException {
        thread = Thread.currentThread();
        return super.awaitCompletion();
    }

    private long downloadedLayerSize() {
        return currentSizes.values().stream().filter(Objects::nonNull).mapToLong(it -> it).sum();
    }

    private long totalLayerSize() {
        return totalSizes.values().stream().filter(Objects::nonNull).mapToLong(it -> it).sum();
    }

    /*
     * This method schedules a future task which will interrupt the waiting thread if ever fired.
     * Every time this method is called (from onStart or onNext), the task is recreated 30s in the future,
     * ensuring that it will only fire if the method stops being called regularly (e.g. if the pull has hung).
     */
    private void resetProgressWatchdog(boolean isFinished) {
        if (nextCheckForProgress != null) {
            nextCheckForProgress.cancel(true);
        }
        if (!isFinished) {
            nextCheckForProgress = PROGRESS_WATCHDOG_EXECUTOR.schedule(() -> {
                logger.error("Docker image pull has not made progress in 30s - aborting pull");
                thread.interrupt();
            }, 30, TimeUnit.SECONDS);
        }
    }
}
