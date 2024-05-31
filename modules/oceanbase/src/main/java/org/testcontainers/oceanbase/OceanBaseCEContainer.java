package org.testcontainers.oceanbase;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for OceanBase Community Edition.
 * <p>
 * Supported image: {@code oceanbase/oceanbase-ce}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>SQL: 2881</li>
 *     <li>RPC: 2882</li>
 * </ul>
 */
public class OceanBaseCEContainer extends JdbcDatabaseContainer<OceanBaseCEContainer> {

    static final String NAME = "oceanbasece";

    static final String DOCKER_IMAGE_NAME = "oceanbase/oceanbase-ce";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(DOCKER_IMAGE_NAME);

    private static final Integer SQL_PORT = 2881;

    private static final Integer RPC_PORT = 2882;

    private static final String DEFAULT_USER_TENANT_NAME = "test";

    private static final String DEFAULT_USER = "root";

    private static final String DEFAULT_PASSWORD = "";

    private static final String DEFAULT_DATABASE_NAME = "test";

    public enum Mode {
        /**
         * Use as much hardware resources as possible for deployment by default,
         * and all environment variables are available.
         */
        NORMAL,
        /**
         * Use the minimum hardware resources for deployment by default,
         * and all environment variables are available.
         */
        MINI,
        /**
         * Use minimal hardware resources and pre-built deployment files for quick startup,
         * and password of user tenant is the only available environment variable.
         */
        SLIM;

        public static Mode fromString(String mode) {
            if (mode == null) {
                throw new IllegalArgumentException("Mode cannot be null");
            }
            switch (mode.toUpperCase()) {
                case "NORMAL":
                    return NORMAL;
                case "MINI":
                    return MINI;
                case "SLIM":
                    return SLIM;
                default:
                    throw new IllegalArgumentException("Unknown mode: " + mode);
            }
        }
    }

    private Mode mode = Mode.SLIM;

    private String tenantName = DEFAULT_USER_TENANT_NAME;

    private String password = DEFAULT_PASSWORD;

    public OceanBaseCEContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public OceanBaseCEContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        addExposedPorts(SQL_PORT, RPC_PORT);
        setWaitStrategy(Wait.forLogMessage(".*boot success!.*", 1));
    }

    @Override
    protected void configure() {
        addEnv("MODE", mode.name().toLowerCase());

        if (!DEFAULT_USER_TENANT_NAME.equals(tenantName)) {
            if (mode == Mode.SLIM) {
                throw new IllegalArgumentException("Tenant name is not configurable on slim mode");
            }
            addEnv("OB_TENANT_NAME", tenantName);
        }

        if (!DEFAULT_PASSWORD.equals(password)) {
            addEnv("OB_TENANT_PASSWORD", password);
        }
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }

    @Override
    public String getDriverClassName() {
        return OceanBaseJdbcUtils.getDriverClass();
    }

    @Override
    public String getJdbcUrl() {
        String additionalUrlParams = constructUrlParameters("?", "&");
        String prefix = OceanBaseJdbcUtils.isMySQLDriver(getDriverClassName()) ? "jdbc:mysql://" : "jdbc:oceanbase://";
        return prefix + getHost() + ":" + getMappedPort(SQL_PORT) + "/" + DEFAULT_DATABASE_NAME + additionalUrlParams;
    }

    @Override
    public String getDatabaseName() {
        return DEFAULT_DATABASE_NAME;
    }

    @Override
    public String getUsername() {
        return DEFAULT_USER + "@" + tenantName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    protected String getTestQueryString() {
        return "SELECT 1";
    }

    public OceanBaseCEContainer withMode(String mode) {
        this.mode = Mode.fromString(mode);
        return this;
    }

    public OceanBaseCEContainer withTenantName(String tenantName) {
        this.tenantName = tenantName;
        return this;
    }

    public OceanBaseCEContainer withPassword(String password) {
        this.password = password;
        return this;
    }
}
