package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public class CrateDBLegacyDriverContainer<SELF extends CrateDBContainer<SELF>> extends CrateDBContainer<SELF> {

    public static final String NAME = "cratedb_legacy";

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
