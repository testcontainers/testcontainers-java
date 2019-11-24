package org.testcontainers.couchbase;

import org.junit.BeforeClass;

public class Couchbase5_1Test extends BaseCouchbaseContainerTest {

    @BeforeClass
    public static void beforeClass() {
        initializeContainerAndBucket("couchbase/server:5.1.1");
    }
}
