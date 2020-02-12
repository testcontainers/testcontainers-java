package org.testcontainers.couchbase;

import org.junit.Rule;

public class Couchbase6_0Test extends BaseCouchbaseContainerFtsTest {

    @Rule
    public CouchbaseContainer container = initCouchbaseContainer("couchbase:6.0.3").withFts(true);

    @Override
    public CouchbaseContainer getCouchbaseContainer() {
        return container;
    }
}
