package org.testcontainers.couchbase;

import com.couchbase.client.core.message.cluster.GetClusterConfigRequest;
import com.couchbase.client.core.message.cluster.GetClusterConfigResponse;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.java.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.rnorth.ducttape.TimeoutException;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;

/**
 * @author ctayeb
 * Created on 06/06/2017
 */
@Slf4j
public class CouchbaseQueryServiceWaitStrategy extends AbstractWaitStrategy {

    private final Bucket bucket;

    public CouchbaseQueryServiceWaitStrategy(Bucket bucket) {
        this.bucket = bucket;
        startupTimeout = Duration.ofSeconds(120);
    }

    @Override
    protected void waitUntilReady() {
        log.info("Waiting for {} seconds for QUERY service", startupTimeout.getSeconds());

        // try to connect to the URL
        try {
            retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                getRateLimiter().doWhenReady(() -> {
                    GetClusterConfigResponse clusterConfig = bucket.core()
                        .<GetClusterConfigResponse>send(new GetClusterConfigRequest())
                        .toBlocking().single();
                    boolean queryServiceEnabled = clusterConfig.config()
                        .bucketConfig(bucket.name())
                        .serviceEnabled(ServiceType.QUERY);
                    if (!queryServiceEnabled) {
                        throw new ContainerLaunchException("Query service not ready yet");
                    }
                });
                return true;
            });
        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for QUERY service");
        }
    }
}
