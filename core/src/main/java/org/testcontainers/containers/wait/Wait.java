package org.testcontainers.containers.wait;

import java.net.HttpURLConnection;

/**
 * Convenience class with logic for building common {@link WaitStrategy} instances.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 *
 * @deprecated Use {@link org.testcontainers.containers.wait.strategy.Wait}
 */
@Deprecated
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
                .forPath(path);
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

    /**
     * Convenience method to return a WaitStrategy for log messages.
     *
     * @param regex the regex pattern to check for
     * @param times the number of times the pattern is expected
     * @return LogMessageWaitStrategy
     */
    public static LogMessageWaitStrategy forLogMessage(String regex, int times) {
        return new LogMessageWaitStrategy().withRegEx(regex).withTimes(times);
    }
}
