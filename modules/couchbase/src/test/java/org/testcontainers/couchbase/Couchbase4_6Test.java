package org.testcontainers.couchbase;

public class Couchbase4_6Test extends BaseCouchbaseContainerTest {
    static {
        imageName = "couchbase/server:4.6.5";
    }
}
