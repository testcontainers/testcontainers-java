package org.testcontainers.couchbase;

import org.junit.BeforeClass;

public class Couchbase4_6Test extends BaseCouchbaseContainerTest {

    @BeforeClass
    public static void beforeClass() {
        initializeContainerAndBucket("couchbase/server:4.6.5");
    }
}
