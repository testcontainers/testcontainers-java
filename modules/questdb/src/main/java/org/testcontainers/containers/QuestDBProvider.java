package org.testcontainers.containers;

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
