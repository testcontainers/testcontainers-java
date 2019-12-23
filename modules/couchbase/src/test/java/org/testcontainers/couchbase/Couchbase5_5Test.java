package org.testcontainers.couchbase;

import org.junit.Rule;

public class Couchbase5_5Test extends BaseCouchbaseContainerTest {

    @Rule
    public CouchbaseContainer container = initCouchbaseContainer("couchbase/server:5.5.1");

    @Override
    public CouchbaseContainer getCouchbaseContainer() {
        return container;
    }
}
