package org.testcontainers.jdbc.mysql;

import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

@ParameterizedClass
@MethodSource("data")
public class MySQLJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                { "jdbc:tc:mysql://hostname/databasename", EnumSet.noneOf(Options.class) },
                {
                    "jdbc:tc:mysql://hostname/databasename?user=someuser&TC_INITSCRIPT=somepath/init_mysql.sql",
                    EnumSet.of(Options.ScriptedSchema, Options.JDBCParams),
                },
                {
                    "jdbc:tc:mysql:8.0.36://hostname/databasename?user=someuser&TC_INITFUNCTION=org.testcontainers.jdbc.AbstractJDBCDriverTest::sampleInitFunction",
                    EnumSet.of(Options.ScriptedSchema, Options.JDBCParams),
                },
                {
                    "jdbc:tc:mysql:8.0.36://hostname/databasename?user=someuser&password=somepwd&TC_INITSCRIPT=somepath/init_mysql.sql",
                    EnumSet.of(Options.ScriptedSchema, Options.JDBCParams),
                },
                {
                    "jdbc:tc:mysql:8.0.36://hostname/databasename?user=someuser&password=somepwd&TC_INITSCRIPT=file:sql/init_mysql.sql",
                    EnumSet.of(Options.ScriptedSchema, Options.JDBCParams),
                },
                {
                    "jdbc:tc:mysql:8.0.36://hostname/databasename?user=someuser&password=somepwd&TC_INITFUNCTION=org.testcontainers.jdbc.AbstractJDBCDriverTest::sampleInitFunction",
                    EnumSet.of(Options.ScriptedSchema, Options.JDBCParams),
                },
                {
                    "jdbc:tc:mysql:8.0.36://hostname/databasename?TC_INITSCRIPT=somepath/init_unicode_mysql.sql&useUnicode=yes&characterEncoding=utf8",
                    EnumSet.of(Options.CharacterSet),
                },
                { "jdbc:tc:mysql:8.0.36://hostname/databasename", EnumSet.noneOf(Options.class) },
                { "jdbc:tc:mysql:8.0.36://hostname/databasename?useSSL=false", EnumSet.noneOf(Options.class) },
                {
                    "jdbc:tc:mysql:8.0.36://hostname/databasename?TC_MY_CNF=somepath/mysql_conf_override",
                    EnumSet.of(Options.CustomIniFile),
                },
            }
        );
    }
}
