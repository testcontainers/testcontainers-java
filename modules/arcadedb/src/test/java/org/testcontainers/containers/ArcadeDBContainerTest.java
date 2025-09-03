package org.testcontainers.containers;

import com.arcadedb.remote.RemoteDatabase;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

public class ArcadeDBContainerTest {

    private static final DockerImageName ARCADEDB_IMAGE = DockerImageName
        .parse("arcadedata/arcadedb:24.4.1")
        .asCompatibleSubstituteFor("arcadedb");

    @Test
    public void shouldReturnTheSameSession() {
        try ( // container {
            ArcadeDBContainer arcadedb = new ArcadeDBContainer("arcadedata/arcadedb:25.3.2")
            // }
        ) {
            arcadedb.start();

            final RemoteDatabase database = arcadedb.getDatabase();
            final RemoteDatabase database2 = arcadedb.getDatabase();

            assertThat(database).isSameAs(database2);
        }
    }

    @Test
    public void shouldInitializeWithCommands() {
        try (ArcadeDBContainer arcadedb = new ArcadeDBContainer(ARCADEDB_IMAGE)) {
            arcadedb.start();

            final RemoteDatabase db = arcadedb.getDatabase();

            db.command("sql", "create vertex type Person");
            db.command("sql", "INSERT INTO Person set name='john'");
            db.command("sql", "INSERT INTO Person set name='jane'");

            assertThat(db.query("sql", "SELECT FROM Person").stream()).hasSize(2);
        }
    }

    @Test
    public void shouldInitializeDatabaseFromScript() {
        try (
            ArcadeDBContainer arcadedb = new ArcadeDBContainer(ARCADEDB_IMAGE)
                .withScriptPath("initscript.sql")
                .withDatabaseName("persons")
        ) {
            arcadedb.start();

            final RemoteDatabase database = arcadedb.getDatabase();

            assertThat(database.query("sql", "SELECT FROM Person").stream()).hasSize(4);
        }
    }
}
