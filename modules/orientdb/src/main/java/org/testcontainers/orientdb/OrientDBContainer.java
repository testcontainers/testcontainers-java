package org.testcontainers.orientdb;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.NonNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

/**
 * Testcontainers implementation for OrientDB.
 * <p>
 * Supported image: {@code orientdb}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Database: 2424</li>
 *     <li>Studio: 2480</li>
 * </ul>
 */
public class OrientDBContainer extends GenericContainer<OrientDBContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("orientdb");

    private static final String DEFAULT_USERNAME = "admin";

    private static final String DEFAULT_PASSWORD = "admin";

    private static final String DEFAULT_SERVER_USER = "root";

    private static final String DEFAULT_SERVER_PASSWORD = "root";

    private static final String DEFAULT_DATABASE_NAME = "testcontainers";

    private static final int DEFAULT_BINARY_PORT = 2424;

    private static final int DEFAULT_HTTP_PORT = 2480;

    private String databaseName;

    private String serverPassword;

    private Transferable scriptPath;

    public OrientDBContainer(@NonNull String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public OrientDBContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        this.serverPassword = DEFAULT_SERVER_PASSWORD;
        this.databaseName = DEFAULT_DATABASE_NAME;

        waitingFor(Wait.forLogMessage(".*OrientDB Studio available.*", 1));
        addExposedPorts(DEFAULT_BINARY_PORT, DEFAULT_HTTP_PORT);
    }

    @Override
    protected void configure() {
        addEnv("ORIENTDB_ROOT_PASSWORD", serverPassword);
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        try {
            String createDb = String.format(
                "CREATE DATABASE remote:localhost/%s %s %s plocal; CONNECT remote:localhost/%s %s %s; CREATE USER %s IDENTIFIED BY %s ROLE admin;",
                this.databaseName,
                DEFAULT_SERVER_USER,
                this.serverPassword,
                this.databaseName,
                DEFAULT_SERVER_USER,
                this.serverPassword,
                DEFAULT_USERNAME,
                DEFAULT_PASSWORD
            );
            execInContainer("/orientdb/bin/console.sh", createDb);

            if (this.scriptPath != null) {
                copyFileToContainer(this.scriptPath, "/opt/testcontainers/script.osql");
                String loadScript = String.format(
                    "CONNECT remote:localhost/%s %s %s; LOAD SCRIPT /opt/testcontainers/script.osql",
                    this.databaseName,
                    DEFAULT_SERVER_USER,
                    this.serverPassword
                );
                execInContainer("/orientdb/bin/console.sh", loadScript);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public OrientDBContainer withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    public OrientDBContainer withServerPassword(final String serverPassword) {
        this.serverPassword = serverPassword;
        return self();
    }

    public OrientDBContainer withScriptPath(Transferable scriptPath) {
        this.scriptPath = scriptPath;
        return self();
    }

    public String getServerUrl() {
        return "remote:" + getHost() + ":" + getMappedPort(2424);
    }

    public String getDbUrl() {
        return getServerUrl() + "/" + this.databaseName;
    }

    public String getServerUser() {
        return DEFAULT_SERVER_USER;
    }

    public String getServerPassword() {
        return this.serverPassword;
    }

    public String getUsername() {
        return DEFAULT_USERNAME;
    }

    public String getPassword() {
        return DEFAULT_PASSWORD;
    }
}
