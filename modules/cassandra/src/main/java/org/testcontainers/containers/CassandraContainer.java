package org.testcontainers.containers;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import com.datastax.oss.driver.internal.core.loadbalancing.DcInferringLoadBalancingPolicy;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.apache.commons.io.IOUtils;
import org.testcontainers.containers.delegate.CassandraDatabaseDelegate;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.ext.ScriptUtils.ScriptLoadException;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import javax.script.ScriptException;

/**
 * Testcontainers implementation for Apache Cassandra.
 * <p>
 * Supported image: {@code cassandra}
 * <p>
 * Exposed ports: 9042
 */
public class CassandraContainer<SELF extends CassandraContainer<SELF>> extends GenericContainer<SELF> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("cassandra");

    private static final String DEFAULT_TAG = "3.11.2";

    @Deprecated
    public static final String IMAGE = DEFAULT_IMAGE_NAME.getUnversionedPart();

    public static final Integer CQL_PORT = 9042;

    private static final String DEFAULT_LOCAL_DATACENTER = "datacenter1";

    private static final String CONTAINER_CONFIG_LOCATION = "/etc/cassandra";

    private static final String USERNAME = "cassandra";

    private static final String PASSWORD = "cassandra";

    private String configLocation;

    private String initScriptPath;

    /**
     * @deprecated use {@link #CassandraContainer(DockerImageName)} instead
     */
    @Deprecated
    public CassandraContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public CassandraContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

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
                DatabaseDelegate databaseDelegate = getDatabaseDelegate();
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
     * Get a session on the configured cluster.
     *
     * Can be used to obtain connections to Cassandra in the container.
     */
    public CqlSession getCqlSession() {
        return getCqlSession(this);
    }

    public static CqlSession getCqlSession(ContainerState containerState) {
        final ProgrammaticDriverConfigLoaderBuilder driverConfigLoaderBuilder = DriverConfigLoader.programmaticBuilder();
        boolean dcAvailable = containerState.getClass().isAssignableFrom(CassandraContainer.class);

        // If the ContainerState is not a CassandraContainer instance, use DcInferringLoadBalancingPolicy to not have
        // to specify the local datacenter to establish the connection, otherwise we can keep the default load balancing
        // policy requiring to explicitly specify the local data center.
        // See https://docs.datastax.com/en/developer/java-driver/latest/manual/core/configuration/reference/ for
        // further information.
        if (!dcAvailable) {
            driverConfigLoaderBuilder.withClass(
                DefaultDriverOption.LOAD_BALANCING_POLICY_CLASS,
                DcInferringLoadBalancingPolicy.class
            );
        }

        // Ignore warnings when a CQL script modifies the current keyspace. Typically, this generates unnecessary logs
        // when executing an init script using multiple keyspaces.
        driverConfigLoaderBuilder.withBoolean(DefaultDriverOption.REQUEST_WARN_IF_SET_KEYSPACE, false);

        // Using Java driver 4.x, a feature called debouncing has been introduced: schema and topology changes received
        // from the server could be accumulated before being processed by the driver. For more information, see:
        // https://docs.datastax.com/en/developer/java-driver/latest/manual/core/performance/index.html#debouncing
        // and https://stackoverflow.com/a/74152732/13292108
        // To maintain good performance, reduce the default values for the schema debouncing properties.
        driverConfigLoaderBuilder.withInt(DefaultDriverOption.METADATA_SCHEMA_MAX_EVENTS, 1);
        driverConfigLoaderBuilder.withDuration(DefaultDriverOption.METADATA_SCHEMA_WINDOW, Duration.ofMillis(1));

        final CqlSessionBuilder cqlSessionBuilder = CqlSession
            .builder()
            .withConfigLoader(driverConfigLoaderBuilder.build())
            .addContactPoint(new InetSocketAddress(containerState.getHost(), containerState.getMappedPort(CQL_PORT)));
        if (dcAvailable) {
            cqlSessionBuilder.withLocalDatacenter(((CassandraContainer<?>) containerState).getLocalDatacenter());
        }
        return cqlSessionBuilder.build();
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

    private DatabaseDelegate getDatabaseDelegate() {
        return new CassandraDatabaseDelegate(this);
    }
}
