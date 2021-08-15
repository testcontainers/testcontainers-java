package org.testcontainers.jdbc.yugabytedb;

import java.util.EnumSet;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import static java.util.Arrays.asList;

/**
 * YugabyteDB YSQL API JDBC connectivity driver test class
 *
 * @author srinivasa-vasu
 */
@RunWith(Parameterized.class)
public class YugabyteYSQLJDBCDriverTest extends AbstractJDBCDriverTest {

	@Parameterized.Parameters(name = "{index} - {0}")
	public static Iterable<Object[]> data() {
		return asList(new Object[][] { { "jdbc:tc:yugabyte://hostname/yugabyte?user=yugabyte&password=yugabyte",
				EnumSet.noneOf(Options.class) }, });
	}

}
