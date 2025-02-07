package org.testcontainers.clickhouse;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.ClickhouseTestImages;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerTest;
import org.testcontainers.utility.DockerImageName;

@RunWith(Parameterized.class)
public class ClickHouseR2DBCDatabaseContainerTest extends AbstractR2DBCDatabaseContainerTest<ClickHouseContainer> {

    @Parameterized.Parameters(name = "{0}")
    public static DockerImageName[] params() {
        return new DockerImageName[]{
            ClickhouseTestImages.CLICKHOUSE_IMAGE,
            ClickhouseTestImages.CLICKHOUSE_24_12_IMAGE
        };
    }

    @Parameterized.Parameter
    public DockerImageName image;

    @Override
    protected ConnectionFactoryOptions getOptions(ClickHouseContainer container) {
        return ClickHouseR2DBCDatabaseContainer.getOptions(container);
    }

    @Override
    protected String createR2DBCUrl() {
        return "r2dbc:tc:clickhouse:///db?TC_IMAGE_TAG=" + image.getVersionPart();
    }

    @Override
    protected ClickHouseContainer createContainer() {
        return new ClickHouseContainer(image);
    }
}
