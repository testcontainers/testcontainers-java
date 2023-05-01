package org.testcontainers.containers;

import lombok.Cleanup;
import org.mockserver.configuration.Configuration;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.socket.tls.KeyStoreFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

public class SimpleHttpClient {

    public static String responseFromMockserver(MockServerContainer mockServer, String path) throws IOException {
        URLConnection urlConnection = new URL(mockServer.getEndpoint() + path).openConnection();
        @Cleanup
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        return reader.readLine();
    }

    public static String secureResponseFromMockserver(MockServerContainer mockServer, String path) throws IOException {
        HttpsURLConnection httpUrlConnection = (HttpsURLConnection) new URL(mockServer.getSecureEndpoint() + path)
            .openConnection();
        try {
            httpUrlConnection.setSSLSocketFactory(
                new KeyStoreFactory(Configuration.configuration(), new MockServerLogger())
                    .sslContext()
                    .getSocketFactory()
            );
            @Cleanup
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpUrlConnection.getInputStream()));
            return reader.readLine();
        } finally {
            httpUrlConnection.disconnect();
        }
    }
}
