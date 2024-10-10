package org.testcontainers.containers;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ObjectClassInformation unit tests
 */
public class ObjectClassInformationTest {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectClassInformationTest.class);

    /**
     * Test the ObjectClassInformation parser
     * <p>
     * We can cheat a bit - due to the way the class is implemented it's enough to ensure that
     * parse() and toString() are consistent.
     * </p>
     *
     * @throws IOException an error occurred when reading the file containing the test data
     */
    @Test
    public void testParser() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("objectClassDefinitions.txt");
        try (InputStream is = url.openStream();
             Reader r = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(r)) {

            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (!line.isEmpty() && !line.startsWith("#")) {
                    final ObjectClassInformation info = ObjectClassInformation.parse(line);
                    assertThat(info.toString()).isEqualTo(line);
                }
            }
        }
    }
}
