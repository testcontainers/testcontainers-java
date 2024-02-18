package org.testcontainers.containers;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for OceanBase.
 * <p>
 * Supported image: {@code oceanbase/oceanbase-ce}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>SQL: 2881</li>
 *     <li>RPC: 2882</li>
 * </ul>
 */
public class OceanBaseContainer extends JdbcDatabaseContainer<OceanBaseContainer> {

    static final String NAME = "oceanbase";

    static final String DOCKER_IMAGE_NAME = "oceanbase/oceanbase-ce";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(DOCKER_IMAGE_NAME);

    private static final Integer SQL_PORT = 2881;
    private static final Integer RPC_PORT = 2882;

    private static final String SYSTEM_TENANT_NAME = "sys";
    private static final String DEFAULT_TEST_TENANT_NAME = "test";
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_DATABASE_NAME = "test";

    private boolean enableFastboot;
    private String mode;
    private String tenantName = DEFAULT_TEST_TENANT_NAME;
    private String driverClassName = "com.mysql.cj.jdbc.Driver";

    public OceanBaseContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public OceanBaseContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        addExposedPorts(SQL_PORT, RPC_PORT);
    }

    @Override
    public String getDriverClassName() {
        return driverClassName;
    }

    @Override
    public String getJdbcUrl() {
        return getJdbcUrl(DEFAULT_DATABASE_NAME);
    }

    public String getJdbcUrl(String databaseName) {
        String additionalUrlParams = constructUrlParameters("?", "&");
        String prefix = driverClassName.contains("mysql") ? "jdbc:mysql://" : "jdbc:oceanbase://";
        return prefix + getHost() + ":" + getMappedPort(SQL_PORT) + "/" + databaseName + additionalUrlParams;
    }

    @Override
    public String getDatabaseName() {
        return DEFAULT_DATABASE_NAME;
    }

    @Override
    public String getUsername() {
        return DEFAULT_USERNAME + "@" + tenantName;
    }

    @Override
    public String getPassword() {
        return DEFAULT_PASSWORD;
    }

    @Override
    protected String getTestQueryString() {
        return "SELECT 1";
    }

    /**
     * Enable fastboot.
     *
     * @return this
     */
    public OceanBaseContainer enableFastboot() {
        this.enableFastboot = true;
        return self();
    }

    /**
     * Set the deployment mode, see <a href="https://hub.docker.com/r/oceanbase/oceanbase-ce">Docker Hub</a> for more details.
     *
     * @param mode the deployment mode
     * @return this
     */
    public OceanBaseContainer withMode(String mode) {
        this.mode = mode;
        return self();
    }

    /**
     * Set the non-system tenant to be created for testing.
     *
     * @param tenantName the name of tenant to be created
     * @return this
     */
    public OceanBaseContainer withTenant(String tenantName) {
        if (StringUtils.isEmpty(tenantName)) {
            throw new IllegalArgumentException("Tenant name cannot be null or empty");
        }
        if (SYSTEM_TENANT_NAME.equals(tenantName)) {
            throw new IllegalArgumentException("Tenant name cannot be " + SYSTEM_TENANT_NAME);
        }
        this.tenantName = tenantName;
        return self();
    }

    /**
     * Set the driver class name.
     *
     * @param driverClassName the driver class name
     * @return this
     */
    public OceanBaseContainer withDriverClassName(String driverClassName) {
        if (StringUtils.isEmpty(driverClassName)) {
            throw new IllegalArgumentException("Driver class name cannot be null or empty");
        }
        if (!driverClassName.contains("mysql") && !driverClassName.contains("oceanbase")) {
            throw new IllegalArgumentException("Driver class name should contains 'mysql' or 'oceanbase'");
        }
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Driver class not found", e);
        }
        this.driverClassName = driverClassName;
        return self();
    }

    @Override
    protected void configure() {
        if (StringUtils.isNotBlank(mode)) {
            withEnv("MODE", mode);
        }
        if (enableFastboot) {
            withEnv("FASTBOOT", "true");
        }
        if (!DEFAULT_TEST_TENANT_NAME.equals(tenantName)) {
            withEnv("OB_TENANT_NAME", tenantName);
        }
    }
}
