package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public class CrateDBLegacyDriverContainer extends CrateDBContainer {

    public static final String NAME = "cratedblegacy";

    public CrateDBLegacyDriverContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    @Override
    public String getDriverClassName() {
        return "io.crate.client.jdbc.CrateDriver";
    }

    @Override
    public String getJdbcUrl() {
        String additionalUrlParams = constructUrlParameters("?", "&");
        return (
            "jdbc:crate://" +
            getHost() +
            ":" +
            getMappedPort(CRATEDB_PG_PORT) +
            "/" +
            getDatabaseName() +
            additionalUrlParams
        );
    }
}
