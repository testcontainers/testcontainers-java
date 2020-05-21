package org.testcontainers.junit.db2;

import org.junit.Test;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class SimpleDb2Test extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (Db2Container db2 = new Db2Container()
            .acceptLicense()) {

            db2.start();

            ResultSet resultSet = performQuery(db2, "SELECT 1 FROM SYSIBM.SYSDUMMY1");

            int resultSetInt = resultSet.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }
}
