package org.testcontainers.tidb;

/**
 * Testcontainers jdbc driver for TiDB.
 */
public enum TiDBJdbcConnectorType {
    MARIA_DB("org.mariadb.jdbc.Driver", "org.mariadb.jdbc.Driver", "mariadb"),
    MYSQL("com.mysql.cj.jdbc.Driver", "com.mysql.jdbc.Driver", "mysql");

    String driverName;
    String legacyDriverName;
    String jdbcPrefix;

    TiDBJdbcConnectorType(String driverName, String legacyDriverName, String jdbcPrefix) {
        this.driverName = driverName;
        this.legacyDriverName = legacyDriverName;
        this.jdbcPrefix = jdbcPrefix;
    }
}
