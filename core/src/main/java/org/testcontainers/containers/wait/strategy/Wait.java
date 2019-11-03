package org.testcontainers.containers.wait.strategy;

import java.net.HttpURLConnection;

/**
 * Convenience class with logic for building common {@link WaitStrategy} instances.
 *
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

    /**
     * Convenience method to return a WaitStrategy leveraging Docker's built-in healthcheck.
     *
     * @return DockerHealthcheckWaitStrategy
     */
    public static DockerHealthcheckWaitStrategy forHealthcheck() {
        return new DockerHealthcheckWaitStrategy();
    }
}
