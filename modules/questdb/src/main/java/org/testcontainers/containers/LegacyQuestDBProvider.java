package org.testcontainers.containers;

@Deprecated
public class LegacyQuestDBProvider extends JdbcDatabaseContainerProvider {

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(QuestDBContainer.LEGACY_DATABASE_PROVIDER);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new QuestDBContainer(QuestDBContainer.DEFAULT_IMAGE_NAME.withTag(tag));
    }
}
