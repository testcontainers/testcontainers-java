package org.testcontainers.questdb;

import org.testcontainers.jdbc.containers.JdbcDatabaseContainer;
import org.testcontainers.jdbc.containers.JdbcDatabaseContainerProvider;

public class QuestDBProvider extends JdbcDatabaseContainerProvider {

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(QuestDBContainer.DATABASE_PROVIDER);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new QuestDBContainer(QuestDBContainer.DEFAULT_IMAGE_NAME.withTag(tag));
    }
}
