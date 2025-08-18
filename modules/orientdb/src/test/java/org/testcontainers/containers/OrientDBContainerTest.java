package org.testcontainers.containers;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import static org.assertj.core.api.Assertions.assertThat;

public class OrientDBContainerTest {

    private static final DockerImageName ORIENTDB_IMAGE = DockerImageName.parse("orientdb:3.2.0-tp3");

    @Test
    public void shouldReturnTheSameSession() {
        try ( // container {
            OrientDBContainer orientdb = new OrientDBContainer("orientdb:3.2.0-tp3")
            // }
        ) {
            orientdb.start();

            final ODatabaseSession session = orientdb.getSession();
            final ODatabaseSession session2 = orientdb.getSession();

            assertThat(session).isSameAs(session2);
        }
    }

    @Test
    public void shouldInitializeWithCommands() {
        try (OrientDBContainer orientdb = new OrientDBContainer(ORIENTDB_IMAGE)) {
            orientdb.start();

            final ODatabaseSession session = orientdb.getSession();

            session.command("CREATE CLASS Person EXTENDS V");
            session.command("INSERT INTO Person set name='john'");
            session.command("INSERT INTO Person set name='jane'");

            assertThat(session.query("SELECT FROM Person").stream()).hasSize(2);
        }
    }

    @Test
    public void shouldQueryWithGremlin() {
        try (
            OrientDBContainer orientdb = new OrientDBContainer(ORIENTDB_IMAGE)
                .withCopyFileToContainer(
                    MountableFile.forClasspathResource("orientdb-server-config.xml"),
                    "/orientdb/config/orientdb-server-config.xml"
                )
        ) {
            orientdb.start();

            final ODatabaseSession session = orientdb.getSession("admin", "admin");

            session.command("CREATE CLASS Person EXTENDS V");
            session.command("INSERT INTO Person set name='john'");
            session.command("INSERT INTO Person set name='jane'");

            assertThat(session.execute("gremlin", "g.V().hasLabel('Person')").stream()).hasSize(2);
        }
    }

    @Test
    public void shouldInitializeDatabaseFromScript() {
        try (
            OrientDBContainer orientdb = new OrientDBContainer(ORIENTDB_IMAGE)
                .withScriptPath("initscript.osql")
                .withDatabaseName("persons")
        ) {
            orientdb.start();

            assertThat(orientdb.getDbUrl())
                .isEqualTo("remote:" + orientdb.getHost() + ":" + orientdb.getMappedPort(2424) + "/persons");

            final ODatabaseSession session = orientdb.getSession();

            assertThat(session.query("SELECT FROM Person").stream()).hasSize(4);
        }
    }
}
