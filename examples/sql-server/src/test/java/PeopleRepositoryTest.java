import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Testcontainers
public class PeopleRepositoryTest {
    @Rule
    public MSSQLServerContainer mssqlserver = new MSSQLServerContainer()
        .acceptLicense();

    @Before
    public void before() {
        mssqlserver.start();
    }

    @After
    public void after() {
        mssqlserver.stop();
    }

    @Test
    public void canGetPeople() throws SQLException {

        String url = mssqlserver.getJdbcUrl();

        String connectionString = String.format("%s;username=%s;password=%s", url, mssqlserver.getUsername(), mssqlserver.getPassword());
        Connection connection = DriverManager.getConnection(connectionString);

        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE DATABASE [test];");
        statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE [test].[dbo].[Person](ID INT PRIMARY KEY, NAME VARCHAR(255) NOT NULL);");
        statement = connection.createStatement();
        statement.executeUpdate("INSERT INTO [test].[dbo].[Person] (ID, NAME) VALUES (1, 'David');");

        List<Person> people = new PersonRepository(connection).getAll();

        Assert.assertEquals(1, people.size());
        Person person = people.get(0);
        Assert.assertEquals(1, person.id);
        Assert.assertEquals("David", person.name);
    }
}
