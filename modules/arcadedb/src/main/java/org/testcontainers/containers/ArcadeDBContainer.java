package org.testcontainers.containers;

import com.arcadedb.remote.RemoteDatabase;
import com.arcadedb.remote.RemoteServer;
import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Testcontainers implementation for ArcadeDB.
 * <p>
 * Supported image: {@code arcadedb} with JDK version up to 17
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Database: 2424</li>
 *     <li>Remote database and Studio: 2480</li>
 * </ul>
 */
public class ArcadeDBContainer extends GenericContainer<ArcadeDBContainer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArcadeDBContainer.class);

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("arcadedata/arcadedb");

    private static final String DEFAULT_SERVER_PASSWORD = "playwithdata";

    private static final String DEFAULT_DATABASE_NAME = "testcontainers";

    private static final int DEFAULT_BINARY_PORT = 2424;

    private static final int DEFAULT_HTTP_PORT = 2480;

    @Getter
    private String databaseName;

    private String serverPassword;

    private int serverPort;

    private Optional<String> scriptPath = Optional.empty();

    private RemoteServer remoteServer;
    private RemoteDatabase database;

    public ArcadeDBContainer(@NonNull String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public ArcadeDBContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        serverPort = DEFAULT_HTTP_PORT;
        serverPassword = DEFAULT_SERVER_PASSWORD;
        databaseName = DEFAULT_DATABASE_NAME;

        waitStrategy = Wait.forLogMessage(".*ArcadeDB Server started.*", 1);

        addExposedPorts(DEFAULT_BINARY_PORT, DEFAULT_HTTP_PORT);
    }

    @Override
    protected void configure() {
        final String javaOpts = String.format("-Darcadedb.server.rootPassword=%s", serverPassword);

        addEnv("JAVA_OPTS", javaOpts);
    }

    public synchronized RemoteDatabase getDatabase() {

        final String host = getHost();
        final Integer port = getMappedPort(serverPort);
        if (remoteServer == null) {
            try {
                remoteServer = new RemoteServer(host, port, "root", serverPassword);
            } catch (Exception e) {
                final String msg = String.format("Could not connect to server %s:%d with user 'root' due to %s",
                    host,  port, e.getMessage());
                LOGGER.error(msg, e);
                throw new IllegalStateException(msg, e);
            }
        }

        if (!remoteServer.exists(getDatabaseName())) {
            remoteServer.create(getDatabaseName());
        }

        if (database != null && database.isOpen()) {
            return database;
        }

        try {
            database = new RemoteDatabase(host, port, getDatabaseName(), "root", serverPassword);
            scriptPath.ifPresent(path -> loadScript(path, database));
            return database;
        } catch (Exception e) {
            final String msg = String.format("Could not connect to database %s on server %s:%d due to %s",
                getDatabaseName(), host, port, e.getMessage());
            LOGGER.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    public ArcadeDBContainer withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    public ArcadeDBContainer withServerPassword(final String serverPassword) {
        this.serverPassword = serverPassword;
        return self();
    }

    public ArcadeDBContainer withServerPort(final int serverPort) {
        this.serverPort = serverPort;
        return self();
    }

    public ArcadeDBContainer withScriptPath(String scriptPath) {
        this.scriptPath = Optional.of(scriptPath);
        return self();
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        final String host = getHost();
        final Integer port = getMappedPort(serverPort);

        try {
            remoteServer = new RemoteServer(host, port, "root", serverPassword);
        } catch (Exception e) {
            final String msg = String.format("Could not connect to server %s:%d with user 'root' due to %s",
                host,  port, e.getMessage());
            LOGGER.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    private void loadScript(String path, RemoteDatabase db) {
        try {
            URL resource = getClass().getClassLoader().getResource(path);

            if (resource == null) {
                LOGGER.warn("Could not load classpath init script: {}", scriptPath);
                throw new RuntimeException(
                    "Could not load classpath init script: " + scriptPath + ". Resource not found."
                );
            }

            String script = IOUtils.toString(resource, StandardCharsets.UTF_8);

            db.command("sqlscript", script);
        } catch (IOException e) {
            LOGGER.warn("Could not load classpath init script: {}", scriptPath);
            throw new RuntimeException("Could not load classpath init script: " + scriptPath, e);
        } catch (UnsupportedOperationException e) {
            LOGGER.error("Error while executing init script: {}", scriptPath, e);
            throw new RuntimeException("Error while executing init script: " + scriptPath, e);
        }
    }
}
