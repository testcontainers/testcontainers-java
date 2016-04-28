package org.testcontainers.containers.wait;

import java.net.HttpURLConnection;

/**
 * Convenience class with logic for building common {@link WaitStrategy} instances.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class Wait {
    /**
     * Convenience method to return the default WaitStrategy.
     *
     * @return a WaitStrategy
     */
    public static WaitStrategy defaultWaitStrategy() {
        return forListeningPort();
    }

    /**
     * Convenience method to return a WaitStrategy for an exposed or mapped port.
     *
     * @return the WaitStrategy
     * @see HostPortWaitStrategy
     */
    public static HostPortWaitStrategy forListeningPort() {
        return new HostPortWaitStrategy();
    }

    /**
     * Convenience method to return a WaitStrategy for an HTTP endpoint.
     *
     * @param path the path to check
     * @return the WaitStrategy
     * @see HttpWaitStrategy
     */
    public static HttpWaitStrategy forHttp(String path) {
        return new HttpWaitStrategy()
                .forPath(path)
                .forStatusCode(HttpURLConnection.HTTP_OK);
    }

    /**
     * Convenience method to return a WaitStrategy for an HTTPS endpoint.
     *
     * @param path the path to check
     * @return the WaitStrategy
     * @see HttpWaitStrategy
     */
    public static HttpWaitStrategy forHttps(String path) {
        return forHttp(path)
                .usingTls();
    }
}
