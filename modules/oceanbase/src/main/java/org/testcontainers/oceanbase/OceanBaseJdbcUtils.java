package org.testcontainers.oceanbase;

import java.util.Arrays;
import java.util.List;

/**
 * Utils for OceanBase Jdbc Connection.
 */
class OceanBaseJdbcUtils {

    static final String MYSQL_JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";

    static final String MYSQL_LEGACY_JDBC_DRIVER = "com.mysql.jdbc.Driver";

    static final String OCEANBASE_JDBC_DRIVER = "com.oceanbase.jdbc.Driver";

    static final String OCEANBASE_LEGACY_JDBC_DRIVER = "com.alipay.oceanbase.jdbc.Driver";

    static final List<String> SUPPORTED_DRIVERS = Arrays.asList(
        OCEANBASE_JDBC_DRIVER,
        OCEANBASE_LEGACY_JDBC_DRIVER,
        MYSQL_JDBC_DRIVER,
        MYSQL_LEGACY_JDBC_DRIVER
    );

    static String getDriverClass() {
        for (String driverClass : SUPPORTED_DRIVERS) {
            try {
                Class.forName(driverClass);
                return driverClass;
            } catch (ClassNotFoundException e) {
                // try to load next driver
            }
        }
        throw new RuntimeException("Can't find valid driver class for OceanBase");
    }

    static boolean isMySQLDriver(String driverClassName) {
        return MYSQL_JDBC_DRIVER.equals(driverClassName) || MYSQL_LEGACY_JDBC_DRIVER.equals(driverClassName);
    }

    static boolean isOceanBaseDriver(String driverClassName) {
        return OCEANBASE_JDBC_DRIVER.equals(driverClassName) || OCEANBASE_LEGACY_JDBC_DRIVER.equals(driverClassName);
    }
}
