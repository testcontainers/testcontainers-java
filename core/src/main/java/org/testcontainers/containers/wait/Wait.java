package org.testcontainers.containers.wait;

import org.testcontainers.containers.output.OutputFrame;

import java.net.HttpURLConnection;
import java.util.function.Predicate;

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

    /**
     * Convenience method to return a WaitStrategy for a generic {@link SimpleWaitStrategy.ContainerReadyCheckFunction}.
     * <p>
     * <b>Example</b>
     * <pre>{@code
     * Wait.until( "send smtp 'HELO' command", container -> {
     *
     *   Container container = new GenericContainer();
     *   Properties props = new Properties();
     *   props.put("mail.smtp.host", container.getContainerIpAddress());
     *   props.put("mail.smtp.port", container.getMappedPort(25));
     *   Session session = Session.getInstance(props);
     *   Transport transport = session.getTransport("smtp");
     *   transport.connect();
     *   transport.close();
     *
     *   return true;
     * })
     * }</pre>
     *
     * @param description        description of for what you are waiting for
     * @param readyCheckFunction {@link SimpleWaitStrategy} will wait for this function to return true,
     *                           {@link Exception}s will be treated as false
     * @return the WaitStrategy
     * @see SimpleWaitStrategy
     */
    public static SimpleWaitStrategy until(String description, SimpleWaitStrategy.ContainerReadyCheckFunction readyCheckFunction) {
        return new SimpleWaitStrategy(description, readyCheckFunction);
    }

    /**
     * Convenience method to return a WaitStrategy for a generic {@link SimpleWaitStrategy.ContainerReadyCheckFunction}.
     * <p>
     * <b>Example</b>
     * <pre>{@code
     * Wait.forOutput( "startup done", frame -> frame.getUtf8String().equals("STARTUP DONE"))
     * }</pre>
     *
     * @param predicate   will wait for this predicate to return true,
     *                    {@link Exception}s will be treated as false
     * @return the WaitStrategy
     * @see SimpleWaitStrategy
     */
    public static OutputWaitStrategy forOutput(Predicate<OutputFrame> predicate) {
        return new OutputWaitStrategy(predicate);
    }
}
