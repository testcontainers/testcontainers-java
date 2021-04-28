package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public class ClickHouseContainer<SELF extends ClickHouseContainer<SELF>> extends GenericContainer<SELF> {

    public ClickHouseContainer() {
        this(ClickHouseInit.DEFAULT_TAG);
    }

    public ClickHouseContainer(String tag) {
        this(ClickHouseInit.DEFAULT_IMAGE_NAME.withTag(tag));
    }

    public ClickHouseContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(ClickHouseInit.DEFAULT_IMAGE_NAME);

        ClickHouseInit.Init(this);
    }

    public String getDatabaseName() {
        return ClickHouseInit.databaseName;
    }

    public String getUsername() {
        return ClickHouseInit.username;
    }

    public String getPassword() {
        return ClickHouseInit.password;
    }

    public String getTestQueryString() {
        return ClickHouseInit.TEST_QUERY;
    }

}
