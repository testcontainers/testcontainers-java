import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PersonRepository {

    private final Connection connection;

    public PersonRepository(Connection connection) {

        this.connection = connection;
    }

    public List<Person> getAll() throws SQLException {
        ResultSet resultSet = connection.createStatement()
            .executeQuery("SELECT * FROM test.dbo.PERSON");
        ArrayList<Person> people = new ArrayList<>();

        while(resultSet.next()) {
            people.add(new Person(resultSet.getInt("ID"), resultSet.getString("Name")));
        }

        return people;
    }
}
