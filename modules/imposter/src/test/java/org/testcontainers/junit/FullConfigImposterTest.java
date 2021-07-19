package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.ImposterContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.support.BaseImposterTest;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThat;

/**
 * Provides a path to a valid Imposter configuration file, which, unlike {@link StandaloneSpecImposterTest} allows
 * full customisation of the Imposter tenant.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class FullConfigImposterTest extends BaseImposterTest {
    @Rule
    public ImposterContainer<?> imposter;

    public FullConfigImposterTest() throws URISyntaxException {
        final Path specDir = Paths.get(FullConfigImposterTest.class.getResource("/specifications").toURI());
        imposter = new ImposterContainer<>()
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withConfigurationDir(specDir);
    }

    @Test
    public void testSpecExampleAsString() throws Exception {
        final URI apiUrl = URI.create(imposter.getBaseUrl() + "/v1/pets");

        assertThat("An HTTP GET from the Imposter server returns the pets JSON array from the specification example",
            responseFromImposter(apiUrl),
            allOf(
                startsWith("["),
                containsString("Cat")
            )
        );
    }
}
