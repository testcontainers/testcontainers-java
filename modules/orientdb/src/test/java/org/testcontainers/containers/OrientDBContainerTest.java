package org.testcontainers.containers;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import org.junit.Test;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author robfrank
 */
public class OrientDBContainerTest {

    private static final DockerImageName ORIENTDB_IMAGE = DockerImageName.parse("orientdb:3.2.0-tp3");

    @Test
    public void shouldReturnTheSameSession() {
        try (OrientDBContainer container = new OrientDBContainer(ORIENTDB_IMAGE)) {
            container.start();

            final ODatabaseSession session = container.getSession();
            final ODatabaseSession session2 = container.getSession();

            assertThat(session).isSameAs(session2);
        }
    }

    @Test
    public void shouldInitializeWithCommands() {
        try (OrientDBContainer container = new OrientDBContainer(ORIENTDB_IMAGE)) {
            container.start();

            final ODatabaseSession session = container.getSession();

            session.command("CREATE CLASS Person EXTENDS V");
            session.command("INSERT INTO Person set name='john'");
            session.command("INSERT INTO Person set name='jane'");

            assertThat(session.query("SELECT FROM Person").stream()).hasSize(2);
        }
    }

    @Test
    public void shouldQueryWithGremlin() throws URISyntaxException, IOException {
        String orientdbServerConfig = new String(
            Files.readAllBytes(
                Paths.get(getClass().getClassLoader().getResource("orientdb-server-config.xml").toURI())
            ),
            StandardCharsets.UTF_8
        );
        try (
            OrientDBContainer container = new OrientDBContainer(ORIENTDB_IMAGE)
                .withCopyToContainer(
                    Transferable.of(orientdbServerConfig),
                    "/orientdb/config/orientdb-server-config.xml"
                )
        ) {
            container.start();

            final ODatabaseSession session = container.getSession("admin", "admin");

            session.command("CREATE CLASS Person EXTENDS V");
            session.command("INSERT INTO Person set name='john'");
            session.command("INSERT INTO Person set name='jane'");

            assertThat(session.execute("gremlin", "g.V().hasLabel('Person')").stream()).hasSize(2);
        }
    }

    @Test
    public void shouldInitializeDatabaseFromScript() {
        try (
            OrientDBContainer container = new OrientDBContainer(ORIENTDB_IMAGE)
                .withScriptPath("initscript.osql")
                .withDatabaseName("persons")
        ) {
            container.start();

            assertThat(container.getDbUrl())
                .isEqualTo("remote:" + container.getHost() + ":" + container.getMappedPort(2424) + "/persons");

            final ODatabaseSession session = container.getSession();

            assertThat(session.query("SELECT FROM Person").stream()).hasSize(4);
        }
    }
}
