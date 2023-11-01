package org.testcontainers.oracle.r2dbc;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.oracle.OracleR2DBCDatabaseContainer;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerTest;

public class OracleR2DBCDatabaseContainerTest extends AbstractR2DBCDatabaseContainerTest<OracleContainer> {

    @Override
    protected OracleContainer createContainer() {
        return new OracleContainer("gvenzl/oracle-free:slim-faststart");
    }

    @Override
    protected ConnectionFactoryOptions getOptions(OracleContainer container) {
        ConnectionFactoryOptions options = OracleR2DBCDatabaseContainer.getOptions(container);

        return options;
    }

    protected String createR2DBCUrl() {
        return "r2dbc:tc:oracle:///db?TC_IMAGE_TAG=slim-faststart";
    }

    @Override
    protected String query() {
        return "SELECT %s from dual";
    }
}
