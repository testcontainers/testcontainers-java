package org.testcontainers.junit.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Cleanup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.util.stream.Collectors;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class BaseImposterTest {
    protected static final Logger logger = LoggerFactory.getLogger(BaseImposterTest.class);
    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected String responseFromImposter(URI baseUrl) throws IOException {
        final URLConnection urlConnection = baseUrl.toURL().openConnection();
        final @Cleanup BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        return reader.lines().collect(Collectors.joining());
    }
}
