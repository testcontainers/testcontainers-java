package org.testcontainers.containers;

import lombok.SneakyThrows;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Simon Schneider
 */
public class JwksContainerTest {

    public final OkHttpClient client = new OkHttpClient();

    @Test
    @SneakyThrows
    public void testJwksJsonAvailability() {
        try (JwksContainer container = new JwksContainer()) {
            container.start();
            Request request = new Request.Builder().url(container.baseUrl() + "/jwks.json").build();
            Response response = client.newCall(request).execute();
            assertNotNull(response.body());
            assertTrue(response.body().string().contains("keys"));
        }
    }
}
