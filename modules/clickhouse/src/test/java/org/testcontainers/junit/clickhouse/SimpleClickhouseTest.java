package org.testcontainers.junit.clickhouse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.ClickhouseTestImages;
import org.testcontainers.containers.ClickHouseContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@ParameterizedClass
@MethodSource("data")
public class SimpleClickhouseTest extends AbstractContainerDatabaseTest {

    private final DockerImageName imageName;

    public SimpleClickhouseTest(DockerImageName imageName) {
        this.imageName = imageName;
    }

    public static Object[][] data() {
        return new Object[][] { //
            { ClickhouseTestImages.CLICKHOUSE_IMAGE },
        };
    }

    @Test
    public void testSimple() throws SQLException {
        try (ClickHouseContainer clickhouse = new ClickHouseContainer(this.imageName)) {
            clickhouse.start();

            ResultSet resultSet = performQuery(clickhouse, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }
}
