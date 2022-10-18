package org.testcontainers.junit.questdb;

import org.junit.Test;
import org.testcontainers.QuestDBTestImages;
import org.testcontainers.containers.QuestDBContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleQuestDBTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (QuestDBContainer questDB = new QuestDBContainer(QuestDBTestImages.QUESTDB_IMAGE)) {
            questDB.start();

            ResultSet resultSet = performQuery(questDB, QuestDBContainer.TEST_QUERY);

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }
}
