package org.testcontainers.couchbase;

import org.junit.ClassRule;

public class Couchbase4_6Test extends BaseCouchbaseContainerTest {
    @ClassRule
    public static CouchbaseContainer container = initCouchbaseContainer("couchbase/server:4.6.5");

    @Override
    public CouchbaseContainer getCouchbaseContainer() {
        return container;
    }
}
