package org.testcontainers.junit;

import lombok.Cleanup;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.ImposterContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThat;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SimpleImposterTest {
    @Rule
    public ImposterContainer imposter;

    public SimpleImposterTest() throws URISyntaxException {
        imposter = new ImposterContainer<>()
            .withSpecificationDir(Paths.get(SimpleImposterTest.class.getResource("/specifications").toURI()).toString());
    }

    @Test
    public void testSpecificationExample() throws Exception {
        final URI apiUrl = URI.create(imposter.getBaseUrl("http", ImposterContainer.IMPOSTER_DEFAULT_PORT) + "/v1/pets");

        assertThat("An HTTP GET from the Imposter server returns the pets JSON array from the specification example",
            responseFromImposter(apiUrl),
            allOf(
                startsWith("["),
                containsString("Cat")
            )
        );
    }

    private static String responseFromImposter(URI baseUrl) throws IOException {
        final URLConnection urlConnection = baseUrl.toURL().openConnection();
        final @Cleanup BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        return reader.lines().collect(Collectors.joining());
    }
}
