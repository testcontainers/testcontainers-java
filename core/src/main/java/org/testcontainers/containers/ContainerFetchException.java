package org.testcontainers.containers;

/**
 * Created by rnorth on 15/10/2015.
 */
public class ContainerFetchException extends RuntimeException {
    public ContainerFetchException(String s, Exception e) {
        super(s, e);
    }

    public ContainerFetchException(String s) {
        super(s);
    }
}
