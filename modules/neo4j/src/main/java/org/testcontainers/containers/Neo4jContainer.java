package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.LicenseAcceptance;
import org.testcontainers.utility.MountableFile;

import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Testcontainers implementation for Neo4j.
 * <p>
 * Supported image: {@code neo4j}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Bolt: 7687</li>
 *     <li>HTTP: 7474</li>
 *     <li>HTTPS: 7473</li>
 * </ul>
 */
public class Neo4jContainer<S extends Neo4jContainer<S>> extends GenericContainer<S> {

    /**
     * The image defaults to the official Neo4j image: <a href="https://hub.docker.com/_/neo4j/">Neo4j</a>.
     */
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("neo4j");

    /**
     * The default tag (version) to use.
     */
    private static final String DEFAULT_TAG = "4.4";

    private static final String ENTERPRISE_TAG = DEFAULT_TAG + "-enterprise";

    /**
     * Default port for the binary Bolt protocol.
     */
    private static final int DEFAULT_BOLT_PORT = 7687;

    /**
     * The port of the transactional HTTPS endpoint: <a href="https://neo4j.com/docs/rest-docs/current/">Neo4j REST API</a>.
     */
    private static final int DEFAULT_HTTPS_PORT = 7473;

    /**
     * The port of the transactional HTTP endpoint: <a href="https://neo4j.com/docs/rest-docs/current/">Neo4j REST API</a>.
     */
    private static final int DEFAULT_HTTP_PORT = 7474;

    /**
     * The official image requires a change of password by default from "neo4j" to something else. This defaults to "password".
     */
    private static final String DEFAULT_ADMIN_PASSWORD = "password";

    private static final String AUTH_FORMAT = "neo4j/%s";

    private final boolean standardImage;

    private String adminPassword = DEFAULT_ADMIN_PASSWORD;

    private final Set<String> labsPlugins = new HashSet<>();

    /**
     * Default wait strategies
     */
    public static final WaitStrategy WAIT_FOR_BOLT = new LogMessageWaitStrategy()
        .withRegEx(String.format(".*Bolt enabled on .*:%d\\.\n", DEFAULT_BOLT_PORT));

    private static final WaitStrategy WAIT_FOR_HTTP = new HttpWaitStrategy()
        .forPort(DEFAULT_HTTP_PORT)
        .forStatusCodeMatching(response -> response == HttpURLConnection.HTTP_OK);

    /**
     * Creates a Neo4jContainer using the official Neo4j docker image.
     * @deprecated use {@link #Neo4jContainer(DockerImageName)} instead
     */
    @Deprecated
    public Neo4jContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * Creates a Neo4jContainer using a specific docker image.
     *
     * @param dockerImageName The docker image to use.
     */
    public Neo4jContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * Creates a Neo4jContainer using a specific docker image.
     *
     * @param dockerImageName The docker image to use.
     */
    public Neo4jContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        this.standardImage = dockerImageName.getUnversionedPart().equals(DEFAULT_IMAGE_NAME.getUnversionedPart());

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        this.waitStrategy =
            new WaitAllStrategy()
                .withStrategy(WAIT_FOR_BOLT)
                .withStrategy(WAIT_FOR_HTTP)
                .withStartupTimeout(Duration.ofMinutes(2));

        addExposedPorts(DEFAULT_BOLT_PORT, DEFAULT_HTTP_PORT, DEFAULT_HTTPS_PORT);
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Stream
            .of(DEFAULT_BOLT_PORT, DEFAULT_HTTP_PORT, DEFAULT_HTTPS_PORT)
            .map(this::getMappedPort)
            .collect(Collectors.toSet());
    }

    @Override
    protected void configure() {
        configureAuth();
        configureLabsPlugins();
        configureWaitStrategy();
    }

    /**
     * Configured via {@link Neo4jContainer#withAdminPassword(String)} or {@link Neo4jContainer#withoutAuthentication()}
     * It is only possible to set the correct auth in the configuration call.
     * Also, the custom methods overrule the set env parameter.
     */
    private void configureAuth() {
        String neo4jAuthEnvKey = "NEO4J_AUTH";
        if (!getEnvMap().containsKey(neo4jAuthEnvKey) || !DEFAULT_ADMIN_PASSWORD.equals(this.adminPassword)) {
            boolean emptyAdminPassword = this.adminPassword == null || this.adminPassword.isEmpty();
            String neo4jAuth = emptyAdminPassword ? "none" : String.format(AUTH_FORMAT, this.adminPassword);
            addEnv(neo4jAuthEnvKey, neo4jAuth);
        }
    }

    /**
     * Configured via {@link Neo4jContainer#withLabsPlugins}.
     * Configuration can only happen in the configuration call because there is no default.
     */
    private void configureLabsPlugins() {
        String neo4jLabsPluginsEnvKey = "NEO4JLABS_PLUGINS";
        if (!getEnv().contains(neo4jLabsPluginsEnvKey) && !this.labsPlugins.isEmpty()) {
            String enabledPlugins =
                this.labsPlugins.stream().map(pluginName -> "\"" + pluginName + "\"").collect(Collectors.joining(","));

            addEnv(neo4jLabsPluginsEnvKey, "[" + enabledPlugins + "]");
        }
    }

    /**
     * Update the default Neo4jContainer wait strategy based on the exposed ports.
     * Still possible to override the startup timeout before starting the container via {@link WaitStrategy#withStartupTimeout(Duration)}.
     */
    private void configureWaitStrategy() {
        List<Integer> exposedPorts = getExposedPorts();
        boolean boltExposed = exposedPorts.contains(DEFAULT_BOLT_PORT);
        boolean httpExposed = exposedPorts.contains(DEFAULT_HTTP_PORT);
        boolean onlyBoltExposed = boltExposed && !httpExposed;
        boolean onlyHttpExposed = !boltExposed && httpExposed;

        if (onlyBoltExposed) {
            this.waitStrategy =
                new WaitAllStrategy().withStrategy(WAIT_FOR_BOLT).withStartupTimeout(Duration.ofMinutes(2));
        } else if (onlyHttpExposed) {
            this.waitStrategy =
                new WaitAllStrategy().withStrategy(WAIT_FOR_HTTP).withStartupTimeout(Duration.ofMinutes(2));
        }
    }

    /**
     * @return Bolt URL for use with Neo4j's Java-Driver.
     */
    public String getBoltUrl() {
        return String.format("bolt://" + getHost() + ":" + getMappedPort(DEFAULT_BOLT_PORT));
    }

    /**
     * @return URL of the transactional HTTP endpoint.
     */
    public String getHttpUrl() {
        return String.format("http://" + getHost() + ":" + getMappedPort(DEFAULT_HTTP_PORT));
    }

    /**
     * @return URL of the transactional HTTPS endpoint.
     */
    public String getHttpsUrl() {
        return String.format("https://" + getHost() + ":" + getMappedPort(DEFAULT_HTTPS_PORT));
    }

    /**
     * Configures the container to use the enterprise edition of the default docker image.
     * <br><br>
     * Please have a look at the <a href="https://neo4j.com/licensing/">Neo4j Licensing page</a>. While the Neo4j
     * Community Edition can be used for free in your projects under the GPL v3 license, Neo4j Enterprise edition
     * needs either a commercial, education or evaluation license.
     *
     * @return This container.
     */
    public S withEnterpriseEdition() {
        if (!standardImage) {
            throw new IllegalStateException(
                String.format("Cannot use enterprise version with alternative image %s.", getDockerImageName())
            );
        }

        setDockerImageName(DEFAULT_IMAGE_NAME.withTag(ENTERPRISE_TAG).asCanonicalNameString());
        LicenseAcceptance.assertLicenseAccepted(getDockerImageName());

        addEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes");

        return self();
    }

    /**
     * Sets the admin password for the default account (which is <pre>neo4j</pre>). A null value or an empty string
     * disables authentication.
     *
     * @param adminPassword The admin password for the default database account.
     * @return This container.
     */
    public S withAdminPassword(final String adminPassword) {
        if (adminPassword != null && adminPassword.length() < 8) {
            logger().warn("Your provided admin password is too short and will not work with Neo4j 5.3+.");
        }
        this.adminPassword = adminPassword;
        return self();
    }

    /**
     * Disables authentication.
     *
     * @return This container.
     */
    public S withoutAuthentication() {
        return withAdminPassword(null);
    }

    /**
     * Copies an existing {@code graph.db} folder into the container. This can either be a classpath resource or a
     * host resource. Please have a look at the factory methods in {@link MountableFile}.
     * <br>
     * If you want to map your database into the container instead of copying them, please use {@code #withClasspathResourceMapping},
     * but this will only work when your test does not run in a container itself.
     * <br>
     * Note: This method only works with Neo4j 3.5.
     * <br>
     * Mapping would work like this:
     * <pre>
     *      &#64;Container
     *      private static final Neo4jContainer databaseServer = new Neo4jContainer&lt;&gt;()
     *          .withClasspathResourceMapping("/test-graph.db", "/data/databases/graph.db", BindMode.READ_WRITE);
     * </pre>
     *
     * @param graphDb The graph.db folder to copy into the container
     * @throws IllegalArgumentException If the database version is not 3.5.
     * @return This container.
     */
    public S withDatabase(MountableFile graphDb) {
        if (!isNeo4jDatabaseVersionSupportingDbCopy()) {
            throw new IllegalArgumentException(
                "Copying database folder is not supported for Neo4j instances with version 4.0 or higher."
            );
        }
        return withCopyFileToContainer(graphDb, "/data/databases/graph.db");
    }

    /**
     * Adds plugins to the given directory to the container. If {@code plugins} denotes a directory, than all of that
     * directory is mapped to Neo4j's plugins. Otherwise, single resources are copied over.
     * <br>
     * If you want to map your plugins into the container instead of copying them, please use {@code #withClasspathResourceMapping},
     * but this will only work when your test does not run in a container itself.
     *
     * @param plugins
     * @return This container.
     */
    public S withPlugins(MountableFile plugins) {
        return withCopyFileToContainer(plugins, "/var/lib/neo4j/plugins/");
    }

    /**
     * Adds Neo4j configuration properties to the container. The properties can be added as in the official Neo4j
     * configuration, the method automatically translate them into the format required by the Neo4j container.
     *
     * @param key   The key to configure, i.e. {@code dbms.security.procedures.unrestricted}
     * @param value The value to set
     * @return This container.
     */
    public S withNeo4jConfig(String key, String value) {
        addEnv(formatConfigurationKey(key), value);
        return self();
    }

    /**
     * @return The admin password for the <code>neo4j</code> account or literal <code>null</code> if auth is disabled.
     */
    public String getAdminPassword() {
        return adminPassword;
    }

    /**
     * Registers one or more {@link Neo4jLabsPlugin} for download and server startup.
     *
     * @param neo4jLabsPlugins The Neo4j plugins that should get started with the server.
     * @return This container.
     * @deprecated {@link Neo4jLabsPlugin} were deprecated due to naming changes that cannot be solved by this enumeration.
     * Please use the {@link Neo4jContainer#withPlugins(String...)} method.
     */
    public S withLabsPlugins(Neo4jLabsPlugin... neo4jLabsPlugins) {
        List<String> pluginNames = Arrays
            .stream(neo4jLabsPlugins)
            .map(plugin -> plugin.pluginName)
            .collect(Collectors.toList());

        this.labsPlugins.addAll(pluginNames);
        return self();
    }

    /**
     * @deprecated Please use {@link Neo4jContainer#withPlugins(String...)} for named plugins.
     */
    public S withLabsPlugins(String... neo4jLabsPlugins) {
        return this.withPlugins(neo4jLabsPlugins);
    }

    /**
     * Registers one or more Neo4j plugins for server startup.
     * The plugins are listed here
     * <ul>
     *     <li><a href="https://neo4j.com/docs/operations-manual/5/configuration/plugins/">Neo4j 5</a></li>
     *     <li><a href="https://neo4j.com/docs/operations-manual/4.4/docker/operations/#docker-neo4jlabs-plugins">Neo4j 4.4</a></li>
     * </ul>
     *
     * @param plugins The Neo4j plugins that should get started with the server.
     * @return This container.
     */
    public S withPlugins(String... plugins) {
        this.labsPlugins.addAll(Arrays.asList(plugins));
        return self();
    }

    private static String formatConfigurationKey(String plainConfigKey) {
        final String prefix = "NEO4J_";

        return String.format("%s%s", prefix, plainConfigKey.replaceAll("_", "__").replaceAll("\\.", "_"));
    }

    private boolean isNeo4jDatabaseVersionSupportingDbCopy() {
        String usedImageVersion = DockerImageName.parse(getDockerImageName()).getVersionPart();
        ComparableVersion usedComparableVersion = new ComparableVersion(usedImageVersion);

        boolean versionSupportingDbCopy =
            usedComparableVersion.isLessThan("4.0") && usedComparableVersion.isGreaterThanOrEqualTo("2");

        if (versionSupportingDbCopy) {
            return true;
        }
        if (!usedComparableVersion.isSemanticVersion()) {
            logger()
                .warn(
                    "Version {} is not a semantic version. The function \"withDatabase\" will fail.",
                    usedImageVersion
                );
            logger().warn("Copying databases is only supported for Neo4j versions 3.5.x");
        }

        return false;
    }

    public S withRandomPassword() {
        return withAdminPassword(UUID.randomUUID().toString());
    }
}
