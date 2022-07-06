package org.testcontainers.containers;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.tuple.Tuple;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
public class FoundationDBContainerTest {

    private final FDB fdb = FDB.selectAPIVersion(710);

    @SneakyThrows
    @Test
    public void shouldExecuteTransactions() {
        try (final FoundationDBContainer foundationDBContainer = new FoundationDBContainer()) {
            foundationDBContainer.start();

            log.debug("Using connection string {}", foundationDBContainer.getConnectionString());

            final Path clusterFilePath = createClusterTempFile(foundationDBContainer);

            try (Database db = fdb.open(clusterFilePath.toString())) {
                // Run an operation on the database
                db.run(tr -> {
                    tr.set(Tuple.from("hello").pack(), Tuple.from("world").pack());
                    return null;
                });

                // Get the value of key 'hello' from the database
                String resultValue = db.run(tr -> {
                    byte[] result = tr.get(Tuple.from("hello").pack()).join();
                    return Tuple.fromBytes(result).getString(0);
                });
                assertEquals("world", resultValue);
            }
            Files.delete(clusterFilePath);
        }
    }

    @SneakyThrows
    @Test
    public void shouldWorkWithReuse() {
        try (final FoundationDBContainer foundationDBContainer = new FoundationDBContainer().withReuse(true)) {
            foundationDBContainer.start();

            log.debug("Using connection string {}", foundationDBContainer.getConnectionString());

            final Path clusterFilePath = createClusterTempFile(foundationDBContainer);

            try (Database db = fdb.open(clusterFilePath.toString())) {
                db.run(tr -> {
                    tr.set(Tuple.from("hello").pack(), Tuple.from("world").pack());
                    return null;
                });
            }
            Files.delete(clusterFilePath);
        }
    }

    @SneakyThrows
    @Test
    public void shouldRunWithSpecificVersion() {
        try (
            final FoundationDBContainer foundationDBContainer = new FoundationDBContainer(
                DockerImageName.parse("foundationdb/foundationdb:6.3.23")
            )
        ) {
            foundationDBContainer.start();

            log.debug("Using connection string {}", foundationDBContainer.getConnectionString());

            final Path clusterFilePath = createClusterTempFile(foundationDBContainer);

            try (Database db = fdb.open(clusterFilePath.toString())) {
                assertNotNull(db);
                // does not actually work to run a transaction with an older version of the API, as only one version can
                // be selected with FDB.selectAPIVersion for the lifetime of the JVM
            }
            Files.delete(clusterFilePath);
        }
    }

    @SneakyThrows
    @Test
    public void example() {
        try (final FoundationDBContainer foundationDBContainer = new FoundationDBContainer()) {
            foundationDBContainer.start();

            final Path clusterFilePath = Files.createTempFile("fdb", ".cluster");
            Files.write(clusterFilePath, foundationDBContainer.getConnectionString().getBytes(StandardCharsets.UTF_8));

            try (Database db = fdb.open(clusterFilePath.toString())) {
                db.run(tr -> {
                    tr.set(Tuple.from("hello").pack(), Tuple.from("world").pack());
                    return null;
                });
            }
        }
    }

    @SneakyThrows
    private Path createClusterTempFile(final FoundationDBContainer foundationDBContainer) {
        final Path clusterFilePath = Files.createTempFile("fdb", ".cluster");
        Files.write(clusterFilePath, foundationDBContainer.getConnectionString().getBytes(StandardCharsets.UTF_8));
        log.debug("Using cluster file {}", clusterFilePath);
        return clusterFilePath;
    }
}
