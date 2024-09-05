package org.testcontainers.junit.databend;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.DatabendTestImages;
import org.testcontainers.databend.DatabendContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class SimpleDatabendTest extends AbstractContainerDatabaseTest {

    private final DockerImageName imageName;

    public SimpleDatabendTest(DockerImageName imageName) {
        this.imageName = imageName;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] data() {
        return new Object[][] { //
            { DatabendTestImages.DATABEND_IMAGE },
        };
    }

    @Test
    public void testSimple() throws SQLException {
        try (DatabendContainer databend = new DatabendContainer(this.imageName)) {
            databend.start();

            ResultSet resultSet = performQuery(databend, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }
}
