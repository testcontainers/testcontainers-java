import io.r2dbc.client.R2dbc;
import org.junit.Test;
import org.testcontainers.containers.R2dbcPostgreSQLContainer;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

/**
 * @author humblehound
 */
public class SimplePostgreSQLTest {

    @Test
    public void testSimple() {
        R2dbcPostgreSQLContainer postgres = new R2dbcPostgreSQLContainer<>();
        postgres.start();
        R2dbc r2dbc = postgres.getR2dbc();
        Integer result = r2dbc.inTransaction(handle -> handle.createUpdate("SELECT 1").execute()).blockFirst();
        assertEquals("A basic SELECT query succeeds", 1, result);
        postgres.stop();
    }

//    @Test
//    public void testExplicitInitScript() throws SQLException {
//        try (R2dbcPostgreSQLContainer postgres = new R2dbcPostgreSQLContainer().withInitScript("somepath/init_postgresql.sql")) {
//            postgres.start();
//
//            ResultSet resultSet = performQuery(postgres, "SELECT foo FROM bar");
//
//            String firstColumnValue = resultSet.getString(1);
//            assertEquals("Value from init script should equal real value", "hello world", firstColumnValue);
//        }
//    }

}
