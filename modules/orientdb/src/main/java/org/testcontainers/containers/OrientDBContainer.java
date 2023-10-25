package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import lombok.NonNull;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(OrientDBContainer.class);

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("orientdb");

    private static final String DEFAULT_TAG = "3.0.24-tp3";

    private static final String DEFAULT_USERNAME = "admin";

    private static final String DEFAULT_PASSWORD = "admin";

    private static final String DEFAULT_SERVER_PASSWORD = "root";

    private static final String DEFAULT_DATABASE_NAME = "testcontainers";

    private static final int DEFAULT_BINARY_PORT = 2424;

    private static final int DEFAULT_HTTP_PORT = 2480;

    private String databaseName;

    private String serverPassword;

    private Optional<String> scriptPath = Optional.empty();

    private OrientDB orientDB;

    private ODatabaseSession session;

    /**
     * @deprecated use {@link #OrientDBContainer(DockerImageName)} instead
     */
    @Deprecated
    public OrientDBContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public OrientDBContainer(@NonNull String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public OrientDBContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        serverPassword = DEFAULT_SERVER_PASSWORD;
        databaseName = DEFAULT_DATABASE_NAME;

        waitStrategy = new LogMessageWaitStrategy().withRegEx(".*OrientDB Studio available.*");

        addExposedPorts(DEFAULT_BINARY_PORT, DEFAULT_HTTP_PORT);
    }

    @Override
    protected void configure() {
        addEnv("ORIENTDB_ROOT_PASSWORD", serverPassword);
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getTestQueryString() {
        return "SELECT FROM V";
    }

    public OrientDBContainer withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    public OrientDBContainer withServerPassword(final String serverPassword) {
        this.serverPassword = serverPassword;
        return self();
    }

    public OrientDBContainer withScriptPath(String scriptPath) {
        this.scriptPath = Optional.of(scriptPath);
        return self();
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        orientDB = new OrientDB(getServerUrl(), "root", serverPassword, OrientDBConfig.defaultConfig());
    }

    @Deprecated
    public OrientDB getOrientDB() {
        return orientDB;
    }

    public String getServerUrl() {
        return "remote:" + getHost() + ":" + getMappedPort(2424);
    }

    public String getDbUrl() {
        return getServerUrl() + "/" + databaseName;
    }

    @Deprecated
    public ODatabaseSession getSession() {
        return getSession(DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }

    @Deprecated
    public synchronized ODatabaseSession getSession(String username, String password) {
        String orientdbVersion = Arrays
            .stream(this.getContainerInfo().getConfig().getEnv())
            .filter(env -> env.startsWith("ORIENTDB_VERSION"))
            .map(env -> env.split("=")[1])
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("no required env var"));
        boolean isGreaterThan32 = new ComparableVersion(orientdbVersion).isGreaterThanOrEqualTo("3.2.0");
        if (isGreaterThan32) {
            String script = String.format(
                "CREATE DATABASE %s plocal users(%s identified by '%s' role admin)",
                databaseName,
                username,
                password
            );
            if (!orientDB.exists(databaseName)) {
                orientDB.execute(script);
            }
        } else {
            orientDB.createIfNotExists(databaseName, ODatabaseType.PLOCAL);
        }
        if (session == null) {
            session = orientDB.open(databaseName, username, password);

            scriptPath.ifPresent(path -> loadScript(path, session));
        }
        return session;
    }

    @Deprecated
    private void loadScript(String path, ODatabaseSession session) {
        try {
            URL resource = getClass().getClassLoader().getResource(path);

            if (resource == null) {
                LOGGER.warn("Could not load classpath init script: {}", scriptPath);
                throw new RuntimeException(
                    "Could not load classpath init script: " + scriptPath + ". Resource not found."
                );
            }

            String script = IOUtils.toString(resource, StandardCharsets.UTF_8);

            session.execute("sql", script);
        } catch (IOException e) {
            LOGGER.warn("Could not load classpath init script: {}", scriptPath);
            throw new RuntimeException("Could not load classpath init script: " + scriptPath, e);
        } catch (UnsupportedOperationException e) {
            LOGGER.error("Error while executing init script: {}", scriptPath, e);
            throw new RuntimeException("Error while executing init script: " + scriptPath, e);
        }
    }
}
