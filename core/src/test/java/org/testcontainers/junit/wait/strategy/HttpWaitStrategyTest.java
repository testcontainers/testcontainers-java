package org.testcontainers.junit.wait.strategy;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.rnorth.ducttape.RetryCountExceededException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Tests for {@link HttpWaitStrategy}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class HttpWaitStrategyTest extends AbstractWaitStrategyTest<HttpWaitStrategy> {
    /**
     * newline sequence indicating end of the HTTP header.
     */
    private static final String NEWLINE = "\r\n";

    private static final String GOOD_RESPONSE_BODY = "Good Response Body";

    /**
     * Expects that the WaitStrategy returns successfully after receiving an HTTP 200 response from the container.
     */
    @Test
    public void testWaitUntilReadyWithSuccess() {
        waitUntilReadyAndSucceed(createShellCommand("200 OK", GOOD_RESPONSE_BODY));
    }

    /**
     * Ensures that HTTP requests made with the HttpWaitStrategy can be enriched with user defined headers,
     * although the test web server does not depend on the header to response with a 200, by checking the
     * logs we can ensure the HTTP request was correctly sent.
     */
    @Test
    public void testWaitUntilReadyWithSuccessWithCustomHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("baz", "boo");
        GenericContainer container = startContainerWithCommand(createShellCommand("200 OK", GOOD_RESPONSE_BODY),
            createHttpWaitStrategy(ready)
                .withHeader("foo", "bar")
                .withHeaders(headers)
        );
        waitUntilReadyAndSucceed(container);

        String logs = container.getLogs();

        assertThat(logs, containsString("foo: bar"));
        assertThat(logs, containsString("baz: boo"));
    }

    /**
     * Expects that the WaitStrategy returns successfully after receiving an HTTP 401 response from the container.
     * This 401 response is checked with a lambda using {@link HttpWaitStrategy#forStatusCodeMatching(Predicate)}
     */
    @Test
    public void testWaitUntilReadyWithUnauthorizedWithLambda() {
        waitUntilReadyAndSucceed(startContainerWithCommand(createShellCommand("401 UNAUTHORIZED", GOOD_RESPONSE_BODY),
            createHttpWaitStrategy(ready)
                .forStatusCodeMatching(it -> it >= 200 && it < 300 || it == 401)
        ));
    }

    /**
     * Expects that the WaitStrategy returns successfully after receiving an HTTP 401 response from the container.
     * This 401 response is checked with many status codes using {@link HttpWaitStrategy#forStatusCode(int)}
     */
    @Test
    public void testWaitUntilReadyWithManyStatusCodes() {
        waitUntilReadyAndSucceed(startContainerWithCommand(createShellCommand("401 UNAUTHORIZED", GOOD_RESPONSE_BODY),
            createHttpWaitStrategy(ready)
                .forStatusCode(300)
                .forStatusCode(401)
                .forStatusCode(500)
        ));
    }

    /**
     * Expects that the WaitStrategy returns successfully after receiving an HTTP 401 response from the container.
     * This 401 response is checked with with many status codes using {@link HttpWaitStrategy#forStatusCode(int)}
     * and a lambda using {@link HttpWaitStrategy#forStatusCodeMatching(Predicate)}
     */
    @Test
    public void testWaitUntilReadyWithManyStatusCodesAndLambda() {
        waitUntilReadyAndSucceed(startContainerWithCommand(createShellCommand("401 UNAUTHORIZED", GOOD_RESPONSE_BODY),
            createHttpWaitStrategy(ready)
                .forStatusCode(300)
                .forStatusCode(500)
                .forStatusCodeMatching(it -> it == 401)
        ));
    }

    /**
     * Expects that the WaitStrategy throws a {@link RetryCountExceededException} after not receiving any of the
     * error code defined with {@link HttpWaitStrategy#forStatusCode(int)}
     * and {@link HttpWaitStrategy#forStatusCodeMatching(Predicate)}
     */
    @Test
    public void testWaitUntilReadyWithTimeoutAndWithManyStatusCodesAndLambda() {
        waitUntilReadyAndTimeout(startContainerWithCommand(createShellCommand("401 UNAUTHORIZED", GOOD_RESPONSE_BODY),
            createHttpWaitStrategy(ready)
                .forStatusCode(300)
                .forStatusCodeMatching(it -> it == 500)
        ));
    }

    /**
     * Expects that the WaitStrategy throws a {@link RetryCountExceededException} after not receiving any of the
     * error code defined with {@link HttpWaitStrategy#forStatusCode(int)}
     * and {@link HttpWaitStrategy#forStatusCodeMatching(Predicate)}. Note that a 200 status code should not
     * be considered as a successful return as not explicitly set.
     * Test case for: https://github.com/testcontainers/testcontainers-java/issues/880
     */
    @Test
    public void testWaitUntilReadyWithTimeoutAndWithLambdaShouldNotMatchOk() {
        waitUntilReadyAndTimeout(startContainerWithCommand(createShellCommand("200 OK", GOOD_RESPONSE_BODY),
            createHttpWaitStrategy(ready)
                .forStatusCodeMatching(it -> it >= 300)
        ));
    }

    /**
     * Expects that the WaitStrategy throws a {@link RetryCountExceededException} after not receiving an HTTP 200
     * response from the container within the timeout period.
     */
    @Test
    public void testWaitUntilReadyWithTimeout() {
        waitUntilReadyAndTimeout(createShellCommand("400 Bad Request", GOOD_RESPONSE_BODY));
    }

    /**
     * Expects that the WaitStrategy throws a {@link RetryCountExceededException} after not the expected response body
     * from the container within the timeout period.
     */
    @Test
    public void testWaitUntilReadyWithTimeoutAndBadResponseBody() {
        waitUntilReadyAndTimeout(createShellCommand("200 OK", "Bad Response"));
    }


    /**
     * Expects the WaitStrategy probing the right port.
     */
    @Test
    public void testWaitUntilReadyWithSpecificPort() {
        waitUntilReadyAndSucceed(startContainerWithCommand(
            createShellCommand("200 OK", GOOD_RESPONSE_BODY, 9090),
            createHttpWaitStrategy(ready)
                .forPort(9090),
            7070, 8080, 9090
        ));
    }

    @Test
    public void testWaitUntilReadyWithTimoutCausedByReadTimeout() {
        waitUntilReadyAndTimeout(
            startContainerWithCommand(createShellCommand("0 Connection Refused", GOOD_RESPONSE_BODY, 9090),
                createHttpWaitStrategy(ready).forPort(9090).withReadTimeout(Duration.ofMillis(1)),
                9090
            ));
    }

    /**
     * @param ready the AtomicBoolean on which to indicate success
     * @return the WaitStrategy under test
     */
    @NotNull
    protected HttpWaitStrategy buildWaitStrategy(final AtomicBoolean ready) {
        return createHttpWaitStrategy(ready)
            .forResponsePredicate(s -> s.equals(GOOD_RESPONSE_BODY));
    }

    /**
     * Create a HttpWaitStrategy instance with a waitUntilReady implementation
     *
     * @param ready Indicates that the WaitStrategy has completed waiting successfully.
     * @return the HttpWaitStrategy instance
     */
    private HttpWaitStrategy createHttpWaitStrategy(final AtomicBoolean ready) {
        return new HttpWaitStrategy() {
            @Override
            protected void waitUntilReady() {
                // blocks until ready or timeout occurs
                super.waitUntilReady();
                ready.set(true);
            }
        };
    }

    private String createShellCommand(String header, String responseBody) {
        return createShellCommand(header, responseBody, 8080);
    }

    private String createShellCommand(String header, String responseBody, int port) {
        int length = responseBody.getBytes().length;
        return "while true; do { echo -e \"HTTP/1.1 " + header + NEWLINE +
            "Content-Type: text/html" + NEWLINE +
            "Content-Length: " + length + NEWLINE + "\";"
            + " echo \"" + responseBody + "\";} | nc -lp " + port + "; done";
    }
}
