package org.testcontainers.containers;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.utility.DockerImageName;

import java.util.HashSet;
import java.util.Set;

/**
 * @author richardnorth
 */
public class MySQLContainer<SELF extends MySQLContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

    public static final String NAME = "mysql";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("mysql");

    @Deprecated
    public static final String DEFAULT_TAG = "5.7.34";

    @Deprecated
    public static final String IMAGE = DEFAULT_IMAGE_NAME.getUnversionedPart();

    static final String DEFAULT_USER = "test";

    static final String DEFAULT_PASSWORD = "test";

    private static final String MY_CNF_CONFIG_OVERRIDE_PARAM_NAME = "TC_MY_CNF";
    public static final Integer MYSQL_PORT = 3306;
    private String databaseName = "test";
    private String username = DEFAULT_USER;
    private String password = DEFAULT_PASSWORD;
    private static final String MYSQL_ROOT_USER = "root";

    /**
     * @deprecated use {@link MySQLContainer(DockerImageName)} instead
     */
    @Deprecated
    public MySQLContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public MySQLContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public MySQLContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        addExposedPort(MYSQL_PORT);
    }


    @NotNull
    @Override
    protected Set<Integer> getLivenessCheckPorts() {
        return new HashSet<>(getMappedPort(MYSQL_PORT));
    }

    @Override
    protected void configure() {
        optionallyMapResourceParameterAsVolume(MY_CNF_CONFIG_OVERRIDE_PARAM_NAME, "/etc/mysql/conf.d",
            "mysql-default-conf");

        addEnv("MYSQL_DATABASE", databaseName);
        if (!MYSQL_ROOT_USER.equalsIgnoreCase(username)) {
            addEnv("MYSQL_USER", username);
        }
        if (password != null && !password.isEmpty()) {
            addEnv("MYSQL_PASSWORD", password);
            addEnv("MYSQL_ROOT_PASSWORD", password);
        } else if (MYSQL_ROOT_USER.equalsIgnoreCase(username)) {
            addEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "yes");
        } else {
            throw new ContainerLaunchException("Empty password can be used only with the root user");
        }
        setStartupAttempts(3);
    }

    @Override
    public String getDriverClassName() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return "com.mysql.cj.jdbc.Driver";
        } catch (ClassNotFoundException e) {
            return "com.mysql.jdbc.Driver";
        }
    }

    @Override
    public String getJdbcUrl() {
        String additionalUrlParams = constructUrlParameters("?", "&");
        return "jdbc:mysql://" + getHost() + ":" + getMappedPort(MYSQL_PORT) +
            "/" + databaseName + additionalUrlParams;
    }

    @Override
    protected String constructUrlForConnection(String queryString) {
        String url = super.constructUrlForConnection(queryString);

        if (!url.contains("useSSL=")) {
            String separator = url.contains("?") ? "&" : "?";
            url = url + separator + "useSSL=false";
        }

        if (!url.contains("allowPublicKeyRetrieval=")) {
            url = url + "&allowPublicKeyRetrieval=true";
        }

        return url;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getTestQueryString() {
        return "SELECT 1";
    }

    public SELF withConfigurationOverride(String s) {
        parameters.put(MY_CNF_CONFIG_OVERRIDE_PARAM_NAME, s);
        return self();
    }

    @Override
    public SELF withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    @Override
    public SELF withUsername(final String username) {
        this.username = username;
        return self();
    }

    @Override
    public SELF withPassword(final String password) {
        this.password = password;
        return self();
    }
}
