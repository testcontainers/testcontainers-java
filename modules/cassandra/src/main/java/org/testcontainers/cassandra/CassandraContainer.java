package org.testcontainers.cassandra;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.apache.commons.io.IOUtils;
import org.testcontainers.cassandra.delegate.CassandraDatabaseDelegate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.ext.ScriptUtils.ScriptLoadException;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Testcontainers implementation for Apache Cassandra.
 * <p>
 * Supported image: {@code cassandra}
 * <p>
 * Exposed ports: 9042
 */
public class CassandraContainer<SELF extends CassandraContainer<SELF>> extends GenericContainer<SELF> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("cassandra");

    public static final Integer CQL_PORT = 9042;

    private static final String DEFAULT_LOCAL_DATACENTER = "datacenter1";

    private static final String CONTAINER_CONFIG_LOCATION = "/etc/cassandra";

    private static final String USERNAME = "cassandra";

    private static final String PASSWORD = "cassandra";

    private String configLocation;

    private String initScriptPath;

    public CassandraContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        addExposedPort(CQL_PORT);

        withEnv("CASSANDRA_SNITCH", "GossipingPropertyFileSnitch");
        withEnv("JVM_OPTS", "-Dcassandra.skip_wait_for_gossip_to_settle=0 -Dcassandra.initial_token=0");
        withEnv("HEAP_NEWSIZE", "128M");
        withEnv("MAX_HEAP_SIZE", "1024M");
        withEnv("CASSANDRA_ENDPOINT_SNITCH", "GossipingPropertyFileSnitch");
        withEnv("CASSANDRA_DC", DEFAULT_LOCAL_DATACENTER);
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
                new CassandraDatabaseDelegate(this).execute(cql, initScriptPath, -1, false, false);
            } catch (IOException e) {
                logger().warn("Could not load classpath init script: {}", initScriptPath);
                throw new ScriptLoadException("Could not load classpath init script: " + initScriptPath, e);
            } catch (ScriptUtils.ScriptStatementFailedException e) {
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
     * By default, Cassandra has authenticator: AllowAllAuthenticator in cassandra.yaml
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
     * By default, Cassandra has authenticator: AllowAllAuthenticator in cassandra.yaml
     * If username and password need to be used, then authenticator should be set as PasswordAuthenticator
     * (through custom Cassandra configuration) and through CQL with default cassandra-cassandra credentials
     * user management should be modified
     */
    public String getPassword() {
        return PASSWORD;
    }

    /**
     * Retrieve an {@link InetSocketAddress} for connecting to the Cassandra container via the driver.
     *
     * @return A InetSocketAddrss representation of this Cassandra container's host and port.
     */
    public InetSocketAddress getContactPoint() {
        return new InetSocketAddress(getHost(), getMappedPort(CQL_PORT));
    }

    /**
     * Retrieve the Local Datacenter for connecting to the Cassandra container via the driver.
     *
     * @return The configured local Datacenter name.
     */
    public String getLocalDatacenter() {
        return getEnvMap().getOrDefault("CASSANDRA_DC", DEFAULT_LOCAL_DATACENTER);
    }

}
