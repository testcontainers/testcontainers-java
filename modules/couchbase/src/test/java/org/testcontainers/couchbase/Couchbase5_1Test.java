package org.testcontainers.couchbase;

public class Couchbase5_1Test extends BaseCouchbaseContainerTest {
    static {
        imageName = "couchbase/server:5.1.0";
    }
}
