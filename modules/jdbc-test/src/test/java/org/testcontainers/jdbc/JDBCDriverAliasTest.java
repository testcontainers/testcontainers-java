package org.testcontainers.jdbc;

import static java.util.Arrays.asList;

import java.sql.SQLException;
import java.util.EnumSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import com.zaxxer.hikari.HikariDataSource;

@RunWith(Parameterized.class)
public class JDBCDriverAliasTest extends JDBCDriverTest {

    private enum Options {
        ScriptedSchema,
        CharacterSet,
        CustomIniFile,
        JDBCParams,
        PmdKnownBroken
    }

    @Parameter
    public String jdbcUrl;
    @Parameter(1)
    public EnumSet<Options> options;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Iterable<Object[]> data() {
        return asList(
            new Object[][]{
                {"jdbc:tc:mysqlserver://hostname:hostport;databaseName=databasename", EnumSet.noneOf(Options.class)},
                {"jdbc:tc:myownmysql://hostname/databasename", EnumSet.noneOf(Options.class)},
                
            });
    }
    
    @Test
    public void shouldInstanciateContainersAccrdingToDynamicAliasDefinition() throws SQLException {
    	ContainerDatabaseDriver.registerAlias("mysqlserver", "sqlserver", "mcmoe/mssqldocker", "latest");
    	ContainerDatabaseDriver.registerAlias("myownmysql", "mysql", "mysql");
        
    	try (HikariDataSource dataSource = getDataSource(jdbcUrl, 1)) {
            performSimpleTest(dataSource);
        }
    }
    
    @Test
    public void shouldInstanciateContainersAccordingToProperties() throws SQLException {
    	ContainerDatabaseDriver.registerAlias("mysqlserver", "sqlserver", "mcmoe/mssqldocker", "latest");
    	ContainerDatabaseDriver.registerAlias("myownmysql", "mysql", "mysql");
        
    	try (HikariDataSource dataSource = getDataSource(jdbcUrl, 1)) {
            performSimpleTest(dataSource);
        }
    }
    
}
