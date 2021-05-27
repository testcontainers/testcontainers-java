package org.testcontainers.containers;

import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Simon Schneider
 */
public class JwksContainerTest {

    public final OkHttpClient client = new OkHttpClient();

    @Test
    @SneakyThrows
    public void testEndpointAvailability() {
        try (JwksContainer container = new JwksContainer()) {
            container.start();
            Response jwksResponse = get(container, "/jwks.json");
            assertEquals(200, jwksResponse.code());
            assertNotNull(jwksResponse.body());
            assertTrue(jwksResponse.body().string().contains("keys"));

            Response openidResponse = get(container, "/.well-known/openid-configuration");
            assertEquals(200, openidResponse.code());
            assertNotNull(openidResponse.body());
            assertTrue(openidResponse.body().string().contains("jwks_uri"));
        }
    }

    @SneakyThrows
    private Response get(JwksContainer container, String path) {
        Request request = new Request.Builder().url(container.baseUrl() + path).build();
        return client.newCall(request).execute();

    }
}
