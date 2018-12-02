package org.testcontainers.images;

import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.String.format;

public class LoggedPullImageResultCallback extends PullImageResultCallback {
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

    public LoggedPullImageResultCallback(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void onStart(final Closeable stream) {
        super.onStart(stream);
        resetProgressWatchdog();

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

            resetProgressWatchdog();
        }

        if (status != null && (status.startsWith("Pulling from") || status.contains("complete"))) {

            long totalSize = totalSizes.values().stream().filter(Objects::nonNull).mapToLong(it -> it).sum();
            long currentSize = currentSizes.values().stream().filter(Objects::nonNull).mapToLong(it -> it).sum();

            int pendingCount = allLayers.size() - downloadedLayers.size();
            String friendlyTotalSize;
            if (pendingCount > 0) {
                friendlyTotalSize = "? MB";
            } else {
                friendlyTotalSize = FileUtils.byteCountToDisplaySize(totalSize);
            }

            logger.info("Pulling image layers: {} pending, {} downloaded, {} extracted, ({}/{})",
                format("%2d", pendingCount),
                format("%2d", downloadedLayers.size()),
                format("%2d", pulledLayers.size()),
                FileUtils.byteCountToDisplaySize(currentSize),
                friendlyTotalSize);
        }
    }

    @Override
    public void onComplete() {
        super.onComplete();

        long totalSize = totalSizes.values().stream().filter(Objects::nonNull).mapToLong(it -> it).sum();
        logger.info("Pull complete ({} layers, {})", allLayers.size(), FileUtils.byteCountToDisplaySize(totalSize));
    }

    private void resetProgressWatchdog() {
        if (nextCheckForProgress != null) {
            nextCheckForProgress.cancel(true);
        }
        nextCheckForProgress = PROGRESS_WATCHDOG_EXECUTOR.schedule(() -> {
            logger.error("Docker image pull has not made progress in 30s");
            thread.interrupt();
        }, 30, TimeUnit.SECONDS);
    }

    @Override
    public PullImageResultCallback awaitCompletion() throws InterruptedException {
        thread = Thread.currentThread();
        return super.awaitCompletion();
    }
}
