package org.testcontainers.couchbase;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.EntityDetails;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpException;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpRequest;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpRequestInterceptor;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * HTTP request interceptor that adds Basic Authentication headers to HTTP requests.
 * <p>
 * This interceptor checks if the "Authorization" header is already present in the request.
 * If not, it adds a Basic Authentication header using the provided username and password.
 * The credentials are Base64 encoded in the format "username:password".
 * </p>
 *
 * <p><b>Example usage:</b></p>
 * <pre>
 * {@code
 * AuthInterceptor interceptor = new AuthInterceptor("admin", "password");
 * // Register with HTTP client to automatically add auth headers
 * }
 * </pre>
 *
 * @see HttpRequestInterceptor
 */
class AuthInterceptor implements HttpRequestInterceptor {

    private final String usr;

    private final String pass;

    /**
     * Constructs a new AuthInterceptor with the specified credentials.
     *
     * @param usr the username for Basic Authentication
     * @param pass the password for Basic Authentication
     */
    public AuthInterceptor(final String usr, final String pass) {
        this.usr = usr;
        this.pass = pass;
    }

    /**
     * Processes the HTTP request by adding a Basic Authentication header if not already present.
     * <p>
     * The method encodes the username and password using Base64 encoding and sets the
     * "Authorization" header with the value "Basic {base64-encoded-credentials}".
     * </p>
     *
     * @param httpRequest the HTTP request to process
     * @param entityDetails the entity details (if any)
     * @param httpContext the HTTP context for the request
     * @throws HttpException if an HTTP protocol error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void process(HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext)
        throws HttpException, IOException {
        if (!httpRequest.containsHeader("Authorization")) {
            String authValue = "Basic " + Base64.getEncoder()
                .encodeToString((usr + ":" + pass).getBytes(StandardCharsets.UTF_8));
            httpRequest.setHeader("Authorization", authValue);
        }
    }
}
