package org.testcontainers.containers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.Run.StatusEnum;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Test;

public class InfluxDBContainerTest {
    private static final String USERNAME = "new-test-user";
    private static final String PASSWORD = "new-test-password";
    private static final String ORG = "new-test-org";
    private static final String BUCKET = "new-test-bucket";
    private static final String RETENTION = "1w";
    private static final String ADMIN_TOKEN = "super-secret-token";

    @Nullable
    private InfluxDBClient influxDBClient = null;

    @After
    public void stopInfluxDBClient() {
        if (this.influxDBClient != null) {
            this.influxDBClient.close();
            this.influxDBClient = null;
        }
    }

    @Test
    public void getUrl() {
        try (final InfluxDBContainer influxDBContainer = new InfluxDBContainer(
            InfluxDBTestUtils.INFLUXDB_V2_TEST_IMAGE)) {

            // Start the container. This step might take some time...
            influxDBContainer.start();
            assertThat(influxDBContainer.isRunning(), is(true));

            final String actual = influxDBContainer.getUrl();

            assertThat(actual, notNullValue());
        }
    }

    @Test
    public void getInfluxDBClient() {
        try (final InfluxDBContainer influxDBContainer = new InfluxDBContainer(
            InfluxDBTestUtils.INFLUXDB_V2_TEST_IMAGE)) {

            // Start the container. This step might take some time...
            influxDBContainer.start();
            assertThat(influxDBContainer.isRunning(), is(true));

            this.influxDBClient = InfluxDBTestUtils.getInfluxDBClient(influxDBContainer);

            assertThat(this.influxDBClient, notNullValue());
            assertThat(this.influxDBClient.ping(), is(true));
        }
    }

    @Test
    public void getBucket() {
        try (final InfluxDBContainer influxDBContainer = new InfluxDBContainer(
            InfluxDBTestUtils.INFLUXDB_V2_TEST_IMAGE)) {
            influxDBContainer
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
                .withOrganization(ORG)
                .withBucket(BUCKET)
                .withRetention(RETENTION)
                .withAdminToken(ADMIN_TOKEN);

            // Start the container. This step might take some time...
            influxDBContainer.start();
            assertThat(influxDBContainer.isRunning(), is(true));

            this.influxDBClient = InfluxDBTestUtils.getInfluxDBClient(influxDBContainer);

            assertThat(this.influxDBClient, notNullValue());

            final Bucket bucket = this.influxDBClient.getBucketsApi().findBucketByName(BUCKET);
            assertThat(bucket, notNullValue());

            assertThat(bucket.getName(), is(BUCKET));
        }
    }

    @Test
    public void queryForWriteAndRead() {
        try (final InfluxDBContainer influxDBContainer = new InfluxDBContainer(
            InfluxDBTestUtils.INFLUXDB_V2_TEST_IMAGE)) {
            influxDBContainer
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
                .withOrganization(ORG)
                .withBucket(BUCKET)
                .withRetention(RETENTION)
                .withAdminToken(ADMIN_TOKEN);

            // Start the container. This step might take some time...
            influxDBContainer.start();
            assertThat(influxDBContainer.isRunning(), is(true));

            this.influxDBClient = InfluxDBTestUtils.getInfluxDBClient(influxDBContainer);

            assertThat(this.influxDBClient, notNullValue());

            try (final WriteApi writeApi = this.influxDBClient.makeWriteApi()) {

                //
                // Write by Data Point
                //
                final Point point = Point.measurement("temperature")
                    .addTag("location", "west")
                    .addField("value", 55.0D)
                    .time(Instant.now().toEpochMilli(), WritePrecision.MS);

                writeApi.writePoint(point);

            }

            //
            // Query data
            //
            final String flux = String.format("from(bucket:\"%s\") |> range(start: 0)", BUCKET);

            final QueryApi queryApi = this.influxDBClient.getQueryApi();

            final FluxTable fluxTable = queryApi.query(flux).get(0);
            final List<FluxRecord> records = fluxTable.getRecords();
            assertThat(records.size(), is(1));

        }
    }
}
