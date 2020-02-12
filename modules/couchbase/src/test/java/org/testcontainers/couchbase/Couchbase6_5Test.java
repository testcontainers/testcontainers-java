package org.testcontainers.couchbase;

import org.junit.Rule;

public class Couchbase6_5Test extends BaseCouchbaseContainerFtsTest {

    @Rule
    public CouchbaseContainer container = initCouchbaseContainer("couchbase:6.5.0").withFts(true);

    @Override
    public CouchbaseContainer getCouchbaseContainer() {
        return container;
    }
}
