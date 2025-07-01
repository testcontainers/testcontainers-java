package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

// junitExample {
@Testcontainers
class Neo4jExampleTest {

    @Container
    private static Neo4jContainer neo4jContainer = new Neo4jContainer(DockerImageName.parse("neo4j:4.4"))
        .withoutAuthentication(); // Disable password

    @Test
    void testSomethingUsingBolt() {
        // Retrieve the Bolt URL from the container
        String boltUrl = neo4jContainer.getBoltUrl();
        try (Driver driver = GraphDatabase.driver(boltUrl, AuthTokens.none()); Session session = driver.session()) {
            long one = session.run("RETURN 1", Collections.emptyMap()).next().get(0).asLong();
            assertThat(one).isEqualTo(1L);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    void testSomethingUsingHttp() throws IOException {
        // Retrieve the HTTP URL from the container
        String httpUrl = neo4jContainer.getHttpUrl();

        URL url = new URL(httpUrl + "/db/data/transaction/commit");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        try (Writer out = new OutputStreamWriter(con.getOutputStream())) {
            out.write("{\"statements\":[{\"statement\":\"RETURN 1\"}]}");
            out.flush();
        }

        assertThat(con.getResponseCode()).isEqualTo(HttpURLConnection.HTTP_OK);
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String expectedResponse =
                "{\"results\":[{\"columns\":[\"1\"],\"data\":[{\"row\":[1],\"meta\":[null]}]}],\"errors\":[]}";
            String response = buffer.lines().collect(Collectors.joining("\n"));
            assertThat(response).isEqualTo(expectedResponse);
        }
    }
}
// }
