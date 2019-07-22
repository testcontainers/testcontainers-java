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
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static java.net.HttpURLConnection.HTTP_OK;

/**
 * @author robfrank
 */
public class OrientDBContainer extends GenericContainer<OrientDBContainer> {

    private static final String DEFAULT_IMAGE_NAME = "orientdb";

    private static final String DEFAULT_TAG = "3.0.22-tp3";

    private static final String DEFAULT_USERNAME = "admin";

    private static final String DEFAULT_PASSWORD = "admin";

    private static final String DEFAULT_DATABASE_NAME = "testcontainers";

    private static final int DEFAULT_BINARY_PORT = 2424;

    private static final int DEFAULT_HTTP_PORT = 2480;

    private static final Logger LOGGER = LoggerFactory.getLogger(OrientDBContainer.class);

    private static final String DOCKER_IMAGE_NAME = DEFAULT_IMAGE_NAME + ":" + DEFAULT_TAG;

    private String databaseName;

    private String username;

    private String password;

    private OrientDB orientDB;

    private ODatabaseSession session;

    private Optional<String> scriptPath = Optional.empty();


    public OrientDBContainer() {
        this(DOCKER_IMAGE_NAME);
    }

    public OrientDBContainer(@NonNull String dockerImageName) {
        super(dockerImageName);

        username = DEFAULT_USERNAME;
        password = DEFAULT_PASSWORD;
        databaseName = DEFAULT_DATABASE_NAME;

        WaitStrategy waitForHttp = new HttpWaitStrategy()
            .forPort(DEFAULT_HTTP_PORT)
            .forStatusCodeMatching(response -> response == HTTP_OK);

        waitStrategy = new WaitAllStrategy()
            .withStrategy(Wait.forListeningPort())
            .withStrategy(waitForHttp)
            .withStartupTimeout(Duration.ofMinutes(2));
    }

    @Override
    protected void configure() {

        addExposedPorts(DEFAULT_BINARY_PORT, DEFAULT_HTTP_PORT);
        addEnv("ORIENTDB_ROOT_PASSWORD", "root");
    }


    public String getDatabaseName() {
        return databaseName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getTestQueryString() {
        return "SELECT FROM V";
    }

    public OrientDBContainer withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    public OrientDBContainer withUsername(final String username) {
        this.username = username;
        return self();
    }

    public OrientDBContainer withPassword(final String password) {
        this.password = password;
        return self();
    }

    public OrientDBContainer withScriptPath(String scriptPath) {

        this.scriptPath = Optional.of(scriptPath);
        return self();
    }


    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        orientDB = new OrientDB(getServerUrl(), "root", "root", OrientDBConfig.defaultConfig());
    }

    public OrientDB getOrientDB() {
        return orientDB;
    }

    public String getServerUrl() {
        return "remote:" + getContainerIpAddress() + ":" + getMappedPort(2424);
    }

    public String getDbUrl() {
        return getServerUrl() + "/" + databaseName;
    }

    public ODatabaseSession getSession() {

        orientDB.createIfNotExists(databaseName, ODatabaseType.PLOCAL);

        if (session == null) {
            session = orientDB.open(databaseName, username, password);

            scriptPath.ifPresent(path -> loadScript(path, session));
        }
        return session;
    }


    private void loadScript(String path, ODatabaseSession session) {
        try {
            URL resource = getClass().getClassLoader().getResource(path);

            if (resource == null) {
                LOGGER.warn("Could not load classpath init script: {}", scriptPath);
                throw new RuntimeException("Could not load classpath init script: " + scriptPath + ". Resource not found.");
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
