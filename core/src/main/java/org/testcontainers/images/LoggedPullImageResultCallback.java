package org.testcontainers.images;

import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.slf4j.Logger;

import java.io.Closeable;
import java.util.*;

import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

/**
 * {@link PullImageResultCallback} with improved logging of pull progress.
 */
class LoggedPullImageResultCallback extends PullImageResultCallback {
    private final Logger logger;

    private Set<String> allLayers = new HashSet<>();
    private Set<String> downloadedLayers = new HashSet<>();
    private Set<String> pulledLayers = new HashSet<>();
    private Map<String, Long> totalSizes = new HashMap<>();
    private Map<String, Long> currentSizes = new HashMap<>();
    private boolean completed;

    public LoggedPullImageResultCallback(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void onStart(final Closeable stream) {
        super.onStart(stream);

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

        if (status != null && status.contains("complete")) {
            completed = true;
        }
    }

    @Override
    public void onComplete() {
        super.onComplete();

        long totalSize = totalLayerSize();

        if (completed) {
            logger.info("Pull complete ({} layers, {})", allLayers.size(), byteCountToDisplaySize(totalSize));
        }
    }

    private long downloadedLayerSize() {
        return currentSizes.values().stream().filter(Objects::nonNull).mapToLong(it -> it).sum();
    }

    private long totalLayerSize() {
        return totalSizes.values().stream().filter(Objects::nonNull).mapToLong(it -> it).sum();
    }
}
