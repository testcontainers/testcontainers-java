package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.apache.commons.io.IOUtils;
import org.testcontainers.containers.delegate.ScyllaDBDatabaseDelegate;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.ext.ScriptUtils.ScriptLoadException;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.script.ScriptException;

/**
 * Testcontainers implementation for ScyllaDB.
 * <p>
 * Supported image: {@code scylladb}
 * <p>
 * Exposed ports: 9042
 */
public class ScyllaDBContainer<SELF extends ScyllaDBContainer<SELF>> extends GenericContainer<SELF> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("scylladb/scylla:5.2.9");

    public static final Integer CQL_PORT = 9042;

    private static final String DEFAULT_LOCAL_DATACENTER = "datacenter1";

    private static final String CONTAINER_CONFIG_LOCATION = "/etc/scylla";

    private static final String USERNAME = "scylladb";

    private static final String PASSWORD = "scylladb";

    private String configLocation;

    private String initScriptPath;

    private boolean enableJmxReporting;

    public ScyllaDBContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public ScyllaDBContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        addExposedPort(CQL_PORT);
        this.enableJmxReporting = false;

        withEnv("CASSANDRA_SNITCH", "GossipingPropertyFileSnitch");
        withEnv("JVM_OPTS", "-Dcassandra.skip_wait_for_gossip_to_settle=0 -Dcassandra.initial_token=0");
        withEnv("HEAP_NEWSIZE", "128M");
        withEnv("MAX_HEAP_SIZE", "1024M");
        withEnv("SCYLLADB_ENDPOINT_SNITCH", "GossipingPropertyFileSnitch");
        withEnv("SCYLLADB_DC", DEFAULT_LOCAL_DATACENTER);
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
                    throw new ScriptLoadException(
                        "Could not load classpath init script: " + initScriptPath + ". Resource not found."
                    );
                }
                String cql = IOUtils.toString(resource, StandardCharsets.UTF_8);
                DatabaseDelegate databaseDelegate = new ScyllaDBDatabaseDelegate(this);
                ScriptUtils.executeDatabaseScript(databaseDelegate, initScriptPath, cql);
            } catch (IOException e) {
                logger().warn("Could not load classpath init script: {}", initScriptPath);
                throw new ScriptLoadException("Could not load classpath init script: " + initScriptPath, e);
            } catch (ScriptException e) {
                logger().error("Error while executing init script: {}", initScriptPath, e);
                throw new ScriptUtils.UncategorizedScriptException(
                    "Error while executing init script: " + initScriptPath,
                    e
                );
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
        Optional
            .ofNullable(resourceLocation)
            .map(MountableFile::forClasspathResource)
            .ifPresent(mountableFile -> withCopyFileToContainer(mountableFile, pathNameInContainer));
    }

    /**
     * Initialize ScyllaDB with the custom overridden ScyllaDB configuration
     * <p>
     * Be aware, that Docker effectively replaces all /etc/sylladb content with the content of config location, so if
     * scylladb.yaml in configLocation is absent or corrupted, then ScyllaDB just won't launch
     *
     * @param configLocation relative classpath with the directory that contains cassandra.yaml and other configuration files
     */
    public SELF withConfigurationOverride(String configLocation) {
        this.configLocation = configLocation;
        return self();
    }

    /**
     * Initialize ScyllaDB with init CQL script
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
     * Initialize ScyllaDB client with JMX reporting enabled or disabled
     */
    public SELF withJmxReporting(boolean enableJmxReporting) {
        this.enableJmxReporting = enableJmxReporting;
        return self();
    }

    /**
     * Get username
     *
     * By default ScyllaDB has authenticator: AllowAllAuthenticator in scylladb.yaml
     * If username and password need to be used, then authenticator should be set as PasswordAuthenticator
     * (through custom ScyllaDB configuration) and through CQL with default scylladb-scylladb credentials
     * user management should be modified
     */
    public String getUsername() {
        return USERNAME;
    }

    /**
     * Get password
     *
     * By default ScyllaDB has authenticator: AllowAllAuthenticator in scylladb.yaml
     * If username and password need to be used, then authenticator should be set as PasswordAuthenticator
     * (through custom Cassandra configuration) and through CQL with default scylladb-scylladb credentials
     * user management should be modified
     */
    public String getPassword() {
        return PASSWORD;
    }

    /**
     * Retrieve an {@link InetSocketAddress} for connecting to the ScyllaDB container via the driver.
     *
     * @return A InetSocketAddrss representation of this ScyllaDB container's host and port.
     */
    public InetSocketAddress getContactPoint() {
        return new InetSocketAddress(getHost(), getMappedPort(CQL_PORT));
    }

    /**
     * Retrieve the Local Datacenter for connecting to the ScyllaDB container via the driver.
     *
     * @return The configured local Datacenter name.
     */
    public String getLocalDatacenter() {
        return getEnvMap().getOrDefault("SCYLLADB_DC", DEFAULT_LOCAL_DATACENTER);
    }
}
