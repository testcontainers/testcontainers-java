package org.testcontainers.junit.timeplus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.TimeplusTestImages;
import org.testcontainers.containers.TimeplusContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class SimpleTimeplusTest extends AbstractContainerDatabaseTest {

    private final DockerImageName imageName;

    public SimpleTimeplusTest(DockerImageName imageName) {
        this.imageName = imageName;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] data() {
        return new Object[][] { //
            { TimeplusTestImages.TIMEPLUS_PROTON_IMAGE },
            { TimeplusTestImages.TIMEPLUS_IMAGE },
        };
    }

    @Test
    public void testSimple() throws SQLException {
        try (TimeplusContainer timeplus = new TimeplusContainer(this.imageName)) {
            timeplus.start();

            ResultSet resultSet = performQuery(timeplus, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }
}
