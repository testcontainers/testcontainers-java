package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public class QuestDBProvider extends JdbcDatabaseContainerProvider {

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(QuestDBContainer.DATABASE_PROVIDER);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new QuestDBContainer(DockerImageName.parse(QuestDBContainer.IMAGE).withTag(tag));
    }
}
