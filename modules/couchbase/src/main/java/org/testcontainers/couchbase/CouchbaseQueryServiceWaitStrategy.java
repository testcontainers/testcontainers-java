package org.testcontainers.couchbase;

import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.java.Bucket;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ctayeb
 * Created on 06/06/2017
 */
@Slf4j
public class CouchbaseQueryServiceWaitStrategy extends CouchbaseServiceWaitStrategy {

    public CouchbaseQueryServiceWaitStrategy(Bucket bucket) {
        super(bucket, ServiceType.QUERY);
    }
}
