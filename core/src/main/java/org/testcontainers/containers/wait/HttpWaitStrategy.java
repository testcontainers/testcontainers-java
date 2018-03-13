package org.testcontainers.containers.wait;

import org.testcontainers.containers.GenericContainer;

import java.util.function.Predicate;

/**
 * Waits until an HTTP(S) endpoint returns a given status code.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 *
 * @deprecated Use {@link org.testcontainers.containers.wait.strategy.HttpWaitStrategy}
 */
@Deprecated
public class HttpWaitStrategy extends GenericContainer.AbstractWaitStrategy {

    private org.testcontainers.containers.wait.strategy.HttpWaitStrategy delegateStrategy = new org.testcontainers.containers.wait.strategy.HttpWaitStrategy();

    /**
     * Waits for the given status code.
     *
     * @param statusCode the expected status code
     * @return this
     */
    public HttpWaitStrategy forStatusCode(int statusCode) {
        delegateStrategy.forStatusCode(statusCode);
        return this;
    }

    /**
     * Waits for the given path.
     *
     * @param path the path to check
     * @return this
     */
    public HttpWaitStrategy forPath(String path) {
        delegateStrategy.forPath(path);
        return this;
    }

    /**
     * Indicates that the status check should use HTTPS.
     *
     * @return this
     */
    public HttpWaitStrategy usingTls() {
        delegateStrategy.usingTls();
        return this;
    }

    /**
     * Authenticate with HTTP Basic Authorization credentials.
     *
     * @param username the username
     * @param password the password
     * @return this
     */
    public HttpWaitStrategy withBasicCredentials(String username, String password) {
        delegateStrategy.withBasicCredentials(username, password);
        return this;
    }

    /**
     * Waits for the response to pass the given predicate
     * @param responsePredicate The predicate to test the response against
     * @return this
     */
    public HttpWaitStrategy forResponsePredicate(Predicate<String> responsePredicate) {
        delegateStrategy.forResponsePredicate(responsePredicate);
        return this;
    }

    @Override
    protected void waitUntilReady() {
        delegateStrategy.waitUntilReady(this.waitStrategyTarget);
    }
}
