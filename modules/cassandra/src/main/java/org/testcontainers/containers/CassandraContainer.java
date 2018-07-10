package org.testcontainers.containers;

import com.datastax.driver.core.Cluster;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.apache.commons.io.IOUtils;
import org.testcontainers.containers.delegate.CassandraDatabaseDelegate;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.ext.ScriptUtils.ScriptLoadException;
import org.testcontainers.utility.MountableFile;

import javax.script.ScriptException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Cassandra container
 *
 * Supports 2.x and 3.x Cassandra versions
 *
 * @author Eugeny Karpov
 */
public class CassandraContainer<SELF extends CassandraContainer<SELF>> extends GenericContainer<SELF> {

    public static final String IMAGE = "cassandra";
    public static final Integer CQL_PORT = 9042;
    private static final String CONTAINER_CONFIG_LOCATION = "/etc/cassandra";
    private static final String USERNAME = "cassandra";
    private static final String PASSWORD = "cassandra";

    private String configLocation;
    private String initScriptPath;

    public CassandraContainer() {
        this(IMAGE + ":latest");
    }

    public CassandraContainer(String dockerImageName) {
        super(dockerImageName);
        addExposedPort(CQL_PORT);
        setStartupAttempts(3);
    }

    @Override
    protected void configure() {
        optionallyMapResourceParameterAsVolume(CONTAINER_CONFIG_LOCATION, configLocation);
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        runInitScriptIfRequired();
    }

    /**
     * Load init script content and apply it to the database if initScriptPath is set
     */
    private void runInitScriptIfRequired() {
        if (initScriptPath != null) {
            try {
                URL resource = Thread.currentThread().getContextClassLoader().getResource(initScriptPath);
                if (resource == null) {
                    logger().warn("Could not load classpath init script: {}", initScriptPath);
                    throw new ScriptLoadException("Could not load classpath init script: " + initScriptPath + ". Resource not found.");
                }
                String cql = IOUtils.toString(resource, StandardCharsets.UTF_8);
                DatabaseDelegate databaseDelegate = getDatabaseDelegate();
                ScriptUtils.executeDatabaseScript(databaseDelegate, initScriptPath, cql);
            } catch (IOException e) {
                logger().warn("Could not load classpath init script: {}", initScriptPath);
                throw new ScriptLoadException("Could not load classpath init script: " + initScriptPath, e);
            } catch (ScriptException e) {
                logger().error("Error while executing init script: {}", initScriptPath, e);
                throw new ScriptUtils.UncategorizedScriptException("Error while executing init script: " + initScriptPath, e);
            }
        }
    }

    /**
     * Map (effectively replace) directory in Docker with the content of resourceLocation if resource location is not null
     *
     * Protected to allow for changing implementation by extending the class
     *
     * @param pathNameInContainer path in docker
     * @param resourceLocation    relative classpath to resource
     */
    protected void optionallyMapResourceParameterAsVolume(String pathNameInContainer, String resourceLocation) {
        Optional.ofNullable(resourceLocation)
                .map(MountableFile::forClasspathResource)
                .ifPresent(mountableFile -> addFileSystemBind(mountableFile.getResolvedPath(), pathNameInContainer, BindMode.READ_WRITE));
    }

    /**
     * Initialize Cassandra with the custom overridden Cassandra configuration
     * <p>
     * Be aware, that Docker effectively replaces all /etc/cassandra content with the content of config location, so if
     * Cassandra.yaml in configLocation is absent or corrupted, then Cassandra just won't launch
     *
     * @param configLocation relative classpath with the directory that contains cassandra.yaml and other configuration files
     */
    public SELF withConfigurationOverride(String configLocation) {
        this.configLocation = configLocation;
        return self();
    }

    /**
     * Initialize Cassandra with init CQL script
     * <p>
     * CQL script will be applied after container is started (see using WaitStrategy)
     *
     * @param initScriptPath relative classpath resource
     */
    public SELF withInitScript(String initScriptPath) {
        this.initScriptPath = initScriptPath;
        return self();
    }

    /**
     * Get username
     *
     * By default Cassandra has authenticator: AllowAllAuthenticator in cassandra.yaml
     * If username and password need to be used, then authenticator should be set as PasswordAuthenticator
     * (through custom Cassandra configuration) and through CQL with default cassandra-cassandra credentials
     * user management should be modified
     */
    public String getUsername() {
        return USERNAME;
    }

    /**
     * Get password
     *
     * By default Cassandra has authenticator: AllowAllAuthenticator in cassandra.yaml
     * If username and password need to be used, then authenticator should be set as PasswordAuthenticator
     * (through custom Cassandra configuration) and through CQL with default cassandra-cassandra credentials
     * user management should be modified
     */
    public String getPassword() {
        return PASSWORD;
    }

    /**
     * Get configured Cluster
     *
     * Can be used to obtain connections to Cassandra in the container
     */
    public Cluster getCluster() {
        return Cluster.builder()
                .addContactPoint(this.getContainerIpAddress())
                .withPort(this.getMappedPort(CQL_PORT))
                .build();
    }

    private DatabaseDelegate getDatabaseDelegate() {
        return new CassandraDatabaseDelegate(this);
    }
}
