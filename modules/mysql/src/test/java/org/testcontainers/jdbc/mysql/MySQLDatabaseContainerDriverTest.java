package org.testcontainers.jdbc.mysql;

import org.junit.Test;
import org.testcontainers.jdbc.ContainerDatabaseDriver;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MySQLDatabaseContainerDriverTest {

    @Test
    public void shouldRespectBothUrlPropertiesAndParameterProperties() throws SQLException {
        ContainerDatabaseDriver driver = new ContainerDatabaseDriver();
        String url = "jdbc:tc:mysql:5.7.22://hostname/databasename?padCharsWithSpace=true";
        Properties properties = new Properties();
        properties.setProperty("maxRows", "1");

        try (Connection connection = driver.connect(url, properties)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE arbitrary_table (length_5_string CHAR(5))");
                statement.execute("INSERT INTO arbitrary_table VALUES ('abc')");
                statement.execute("INSERT INTO arbitrary_table VALUES ('123')");

                // Check that maxRows is set
                try (ResultSet resultSet = statement.executeQuery("SELECT * FROM arbitrary_table")) {
                    resultSet.next();
                    assertTrue(resultSet.isFirst());
                    assertTrue(resultSet.isLast());
                }

                // Check that pad with chars is set
                try (ResultSet resultSet = statement.executeQuery("SELECT * FROM arbitrary_table")) {
                    assertTrue(resultSet.next());
                    assertEquals("abc  ", resultSet.getString(1));
                }
            }
        }
    }
}
