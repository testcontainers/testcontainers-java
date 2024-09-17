package org.testcontainers.containers;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * TestContainer for Vertica CE databases
 * <p>
 * todo - document vsql
 * </p>
 * <p>
 * todo - add TLS support
 * </p>
 * <p>
 * todo- "deploy: mode: global" ??
 * </p>
 *
 * {@see https://www.vertica.com/}
 * {@see https://docs.vertica.com/12.0.x/en/getting-started/introducing-vmart-example-db/}
 * {@see https://www.microfocus.com/en-us/legal/software-licensing}
 *
 * @param <SELF> this class
 */
@Getter
public class VerticaCEContainer<SELF extends VerticaCEContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

    private static final Logger LOG = LoggerFactory.getLogger(VerticaCEContainer.class);

    private static final String JDBC_SUBPROTOCOL = "vertica";

    private static final String TEST_QUERY_STRING = "SELECT 1";

    private static final String JDBC_DRIVER_CLASSNAME = "com.vertica.jdbc.Driver";

    /** Short name of database container */
    public static final String NAME = "Vertica";

    /** Default image name */
    public static final String IMAGE = "vertica/vertica-ce";

    /** Default version */
    public static final String DEFAULT_TAG = "24.1.0-0";

    /** Default full image name */
    public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(IMAGE);

    /** Default Vertica database port */
    public static final Integer VERTICA_DATABASE_PORT = 5433;

    /** Default Vertica database and data warehouse ports */
    public static final Integer[] VERTICA_PORTS = { VERTICA_DATABASE_PORT, 5444 };

    /** Docker image's default database name */
    public static final String DEFAULT_DATABASE = "vmart";

    /** Docker image's default user name */
    public static final String DEFAULT_APP_DB_USER = "dbadmin";

    /** Docker image's default timezone */
    public static final String DEFAULT_TZ = "Europe/Prague";

    /** Docker image's default password */
    static final String DEFAULT_APP_DB_PASSWORD = "vertica";

    private String databaseName = DEFAULT_DATABASE;

    private String username = DEFAULT_APP_DB_USER;

    private String password = DEFAULT_APP_DB_PASSWORD;

    private String tz = DEFAULT_TZ;

    private Integer loginTimeout;

    private String keyStorePath;

    private String keyStorePassword;

    private String trustStorePath;

    private String trustStorePassword;

    /**
     * Default constructor
     */
    public VerticaCEContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * Constructor taking image name
     *
     * @param dockerImageName image name
     */
    public VerticaCEContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        this.waitStrategy =
            new LogMessageWaitStrategy()
                .withRegEx(".*Vertica is now running.*\\s")
                .withStartupTimeout(Duration.ofMinutes(5));

        for (Integer port : VERTICA_PORTS) {
            this.addExposedPort(port);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        configureDockerContainer();
        configureUrlParameters();
    }

    /**
     * Configure docker container
     */
    void configureDockerContainer() {
        if (!DEFAULT_APP_DB_USER.equals(username)) {
            addEnv("APP_DB_USER", username);
        }

        if (!DEFAULT_APP_DB_PASSWORD.equals(password)) {
            addEnv("APP_DB_PASSWORD", password);
        }

        if (!DEFAULT_TZ.equals(tz)) {
            addEnv("TZ", tz);
        }
    }

    /**
     * Configure UrlParameters
     */
    void configureUrlParameters() {
        urlParameters.put("user", username);
        if (StringUtils.isNotBlank(password)) {
            urlParameters.put("password", password);
        }

        if (loginTimeout != null) {
            urlParameters.put("loginTimeout", Integer.toString(loginTimeout));
        }

        if (StringUtils.isNotBlank(keyStorePath) && StringUtils.isNotBlank(keyStorePassword)) {
            urlParameters.put("KeyStorePath", keyStorePath);
            urlParameters.put("KeyStorePassword", keyStorePassword);
        }

        if (StringUtils.isNotBlank(trustStorePath) && StringUtils.isNotBlank(trustStorePassword)) {
            urlParameters.put("TrustStorePath", trustStorePath);
            urlParameters.put("TrustStorePassword", trustStorePassword);
        }
    }

    /**
     * Get recommended driver classname
     *
     * @return recommended driver classname
     */
    @Override
    public String getDriverClassName() {
        return JDBC_DRIVER_CLASSNAME;
    }

    /**
     * Get container's JDBC URL
     *
     * @return container's JDBC URL
     */
    @Override
    public String getJdbcUrl() {
        final String additionalUrlParams = constructUrlParameters("?", "&");
        return String.format(
            "jdbc:%s://%s:%s/%s%s",
            JDBC_SUBPROTOCOL,
            getHost(),
            getMappedPort(VERTICA_DATABASE_PORT),
            databaseName,
            additionalUrlParams
        );
    }

    /**
     * Get test query string (for DataSources)
     *
     * @return test query string
     */
    @Override
    public String getTestQueryString() {
        return TEST_QUERY_STRING;
    }

    /**
     * Specify custom timezone
     *
     * @param tz time zone (default "Europe/Prague")
     * @return this object
     */
    public SELF withTZ(final String tz) {
        this.tz = tz;
        return self();
    }

    /**
     * Specify custom database name
     *
     * @param databaseName database name
     * @return this object
     */
    @Override
    public SELF withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    /**
     * Specify custom database username
     *
     * @param username username
     * @return this object
     */
    @Override
    public SELF withUsername(final String username) {
        this.username = username;
        return self();
    }

    /**
     * Specify custom database user password
     *
     * @param password user password
     * @return this object
     */
    @Override
    public SELF withPassword(final String password) {
        this.password = password;
        return self();
    }

    /**
     * Specify login timeout
     *
     * @param loginTimeout timeout (in seconds?)
     * @return this object
     */
    public SELF withLoginTimeout(final Integer loginTimeout) {
        this.loginTimeout = loginTimeout;
        return self();
    }

    /**
     * Specify keyStore path. Must be JKS file.
     *
     * TODO: verify file exists and is JKS file.
     *
     * @param keyStorePath path to keyStore file (JKS)
     * @return this object
     */
    public SELF withKeyStorePath(final String keyStorePath) {
        this.keyStorePath = keyStorePath;
        return self();
    }

    /**
     * Specify keyStore password
     *
     * @param keyStorePassword keyStore password
     * @return this object
     */
    public SELF withKeyStorePassword(final String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
        return self();
    }

    /**
     * Specify trustStore path. Must be JKS file.
     *
     * TODO: verify file exists and is JKS file.
     *
     * @param trustStorePath path to trustStore file (JKS)
     * @return this object
     */
    public SELF withTrustStorePath(final String trustStorePath) {
        this.trustStorePath = trustStorePath;
        return self();
    }

    /**
     * Specify trustStore password
     *
     * @param trustStorePassword truststore password
     * @return this object
     */
    public SELF withTrustStorePassword(final String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
        return self();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }
}
