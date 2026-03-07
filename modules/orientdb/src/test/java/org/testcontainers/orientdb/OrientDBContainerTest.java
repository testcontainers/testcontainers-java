package org.testcontainers.orientdb;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import static org.assertj.core.api.Assertions.assertThat;

class OrientDBContainerTest {

    private static final DockerImageName ORIENTDB_IMAGE = DockerImageName.parse("orientdb:3.2.0-tp3");

    @Test
    void shouldInitializeWithCommands() {
        try ( // container {
            OrientDBContainer orientdb = new OrientDBContainer("orientdb:3.2.0-tp3")
            // }
        ) {
            orientdb.start();

            OrientDB orientDB = new OrientDB(
                orientdb.getServerUrl(),
                orientdb.getServerUser(),
                orientdb.getServerPassword(),
                OrientDBConfig.defaultConfig()
            );
            ODatabaseSession session = orientDB.open(
                orientdb.getDatabaseName(),
                orientdb.getUsername(),
                orientdb.getPassword()
            );

            session.command("CREATE CLASS Person EXTENDS V");
            session.command("INSERT INTO Person set name='john'");
            session.command("INSERT INTO Person set name='jane'");

            assertThat(session.query("SELECT FROM Person").stream()).hasSize(2);
        }
    }

    @Test
    void shouldQueryWithGremlin() {
        try (
            OrientDBContainer orientdb = new OrientDBContainer(ORIENTDB_IMAGE)
                .withCopyFileToContainer(
                    MountableFile.forClasspathResource("orientdb-server-config.xml"),
                    "/orientdb/config/orientdb-server-config.xml"
                )
        ) {
            orientdb.start();

            OrientDB orientDB = new OrientDB(
                orientdb.getServerUrl(),
                orientdb.getServerUser(),
                orientdb.getServerPassword(),
                OrientDBConfig.defaultConfig()
            );
            ODatabaseSession session = orientDB.open(
                orientdb.getDatabaseName(),
                orientdb.getUsername(),
                orientdb.getPassword()
            );

            session.command("CREATE CLASS Person EXTENDS V");
            session.command("INSERT INTO Person set name='john'");
            session.command("INSERT INTO Person set name='jane'");

            assertThat(session.execute("gremlin", "g.V().hasLabel('Person')").stream()).hasSize(2);
        }
    }

    @Test
    void shouldInitializeDatabaseFromScript() {
        try (
            OrientDBContainer orientdb = new OrientDBContainer(ORIENTDB_IMAGE)
                .withScriptPath(MountableFile.forClasspathResource("initscript.osql"))
                .withDatabaseName("persons")
        ) {
            orientdb.start();

            assertThat(orientdb.getDbUrl())
                .isEqualTo("remote:" + orientdb.getHost() + ":" + orientdb.getMappedPort(2424) + "/persons");

            OrientDB orientDB = new OrientDB(
                orientdb.getServerUrl(),
                orientdb.getServerUser(),
                orientdb.getServerPassword(),
                OrientDBConfig.defaultConfig()
            );
            ODatabaseSession session = orientDB.open(
                orientdb.getDatabaseName(),
                orientdb.getUsername(),
                orientdb.getPassword()
            );

            assertThat(session.query("SELECT FROM Person").stream()).hasSize(4);
        }
    }
}
