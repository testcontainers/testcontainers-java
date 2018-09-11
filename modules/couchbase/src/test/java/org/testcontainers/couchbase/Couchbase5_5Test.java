package org.testcontainers.couchbase;

import org.junit.ClassRule;

public class Couchbase5_5Test extends BaseCouchbaseContainerTest {
    @ClassRule
    public static CouchbaseContainer container = initCouchbaseContainer("couchbase/server:5.5.1");

    @Override
    public CouchbaseContainer getCouchbaseContainer() {
        return container;
    }
}
