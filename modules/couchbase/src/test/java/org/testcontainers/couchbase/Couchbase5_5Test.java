package org.testcontainers.couchbase;

import org.junit.BeforeClass;

public class Couchbase5_5Test extends BaseCouchbaseContainerTest {

    @BeforeClass
    public static void beforeClass() {
        initializeContainerAndBucket("couchbase/server:5.5.1");
    }
}
