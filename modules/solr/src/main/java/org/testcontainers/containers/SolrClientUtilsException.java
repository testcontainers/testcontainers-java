package org.testcontainers.containers;

public class SolrClientUtilsException extends RuntimeException {
    public SolrClientUtilsException(int statusCode, String msg) {
        super("Http Call Status: " + statusCode + "\n" + msg);
    }
}
