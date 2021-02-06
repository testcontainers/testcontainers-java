package org.testcontainers.containers;

import lombok.Cleanup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class SimpleHttpClient {
    public static String responseFromMockserver(MockServerContainer mockServer, String path) throws IOException {
        URLConnection urlConnection = new URL(mockServer.getEndpoint() + path).openConnection();
        @Cleanup BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        return reader.readLine();
    }
}
