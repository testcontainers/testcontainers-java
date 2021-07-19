package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.ImposterContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.support.BaseImposterTest;
import org.testcontainers.junit.support.Pet;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThat;

/**
 * Constructs a mock directly from an OpenAPI/Swagger specification file.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class StandaloneSpecImposterTest extends BaseImposterTest {
    @Rule
    public ImposterContainer<?> imposter;

    public StandaloneSpecImposterTest() throws URISyntaxException {
        final Path specDir = Paths.get(FullConfigImposterTest.class.getResource("/specifications").toURI());
        imposter = new ImposterContainer<>()
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withSpecificationFile(specDir.resolve("petstore-simple.yaml"));
    }

    @Test
    public void testSpecExampleAsModel() throws Exception {
        final URI apiUrl = URI.create(imposter.getBaseUrl() + "/v1/pets");
        final Pet[] response = MAPPER.readValue(responseFromImposter(apiUrl), Pet[].class);

        assertThat("An HTTP GET from the Imposter server returns the pets array from the specification example",
            response.length,
            equalTo(2)
        );
        assertThat("The first item in the pets array is a cat",
            response[0].getName(),
            equalTo("Cat")
        );
    }
}
