package org.testcontainers.oceanbase;

import org.testcontainers.containers.JdbcDatabaseContainer;
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

    private static final String DEFAULT_TEST_TENANT_NAME = "test";

    private static final String DEFAULT_USERNAME = "root";

    private static final String DEFAULT_PASSWORD = "";

    private static final String DEFAULT_DATABASE_NAME = "test";

    public OceanBaseCEContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public OceanBaseCEContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        addExposedPorts(SQL_PORT, RPC_PORT);
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
        // In OceanBase, the jdbc username is related to the name of user, tenant and cluster,
        // if a tenant name other than the default value 'test' is used, you should manually
        // construct the jdbc username by yourself.
        return DEFAULT_USERNAME + "@" + DEFAULT_TEST_TENANT_NAME;
    }

    @Override
    public String getPassword() {
        return DEFAULT_PASSWORD;
    }

    @Override
    protected String getTestQueryString() {
        return "SELECT 1";
    }
}
