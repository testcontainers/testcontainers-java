package org.testcontainers.couchbase;

import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.java.Bucket;
import lombok.extern.slf4j.Slf4j;

/**
 * @author tchlyah
 * Created on 12/02/2020
 */
@Slf4j
public class CouchbaseQueryServiceWaitStrategy extends CouchbaseServiceWaitStrategy {

    public CouchbaseQueryServiceWaitStrategy(Bucket bucket) {
        super(bucket, ServiceType.QUERY);
    }
}
