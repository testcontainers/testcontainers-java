package org.testcontainers.containers;


import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.BucketRetentionRules;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

public class InfluxDBContainerTest {

    private static final String USERNAME = "new-test-user";

    private static final String PASSWORD = "new-test-password";

    private static final String ORG = "new-test-org";

    private static final String BUCKET = "new-test-bucket";

    private static final String RETENTION = "1w";

    private static final String ADMIN_TOKEN = "super-secret-token";

    private static final int SECONDS_IN_WEEK = 604800;

    @Test
    public void getInfluxDBClient() {
        try (
            // constructorWithDefaultVariables {
            final InfluxDBContainer<?> influxDBContainer = new InfluxDBContainer<>(
                DockerImageName.parse("influxdb:2.0.7"))
            // }
        ) {
            influxDBContainer.start();

            try (final InfluxDBClient influxDBClient = InfluxDBTestUtils.createInfluxDBClient(influxDBContainer)) {
                Assertions.assertThat(influxDBClient).isNotNull();
                Assertions.assertThat(influxDBClient.ping()).isTrue();
            }
        }
    }

    @Test
    public void getInfluxDBClientWithAdminToken() {
        try (
            // constructorWithAdminToken {
            final InfluxDBContainer<?> influxDBContainer = new InfluxDBContainer<>(
                DockerImageName.parse("influxdb:2.0.7")).withAdminToken(ADMIN_TOKEN)
            // }
        ) {
            influxDBContainer.start();
            final Optional<String> adminToken = influxDBContainer.getAdminToken();
            Assertions.assertThat(adminToken).isNotEmpty();

            try (final InfluxDBClient influxDBClient = InfluxDBTestUtils.createInfluxDBClientWithToken(
                influxDBContainer.getUrl(), adminToken.get())) {
                Assertions.assertThat(influxDBClient).isNotNull();
                Assertions.assertThat(influxDBClient.ping()).isTrue();
            }
        }
    }

    @Test
    public void getBucket() {
        try (
            // constructorWithCustomVariables {
            final InfluxDBContainer<?> influxDBContainer = new InfluxDBContainer<>(
                DockerImageName.parse("influxdb:2.0.7"))
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
                .withOrganization(ORG)
                .withBucket(BUCKET)
                .withRetention(RETENTION);
            // }
        ) {

            influxDBContainer.start();

            try (final InfluxDBClient influxDBClient = InfluxDBTestUtils.createInfluxDBClient(influxDBContainer)) {
                final Bucket bucket = influxDBClient.getBucketsApi().findBucketByName(BUCKET);
                Assertions.assertThat(bucket).isNotNull();

                Assertions.assertThat(bucket.getName()).isEqualTo(BUCKET);
                Assertions.assertThat(bucket.getRetentionRules()).
                    hasSize(1)
                    .first()
                    .extracting(BucketRetentionRules::getEverySeconds)
                    .isEqualTo(SECONDS_IN_WEEK);
            }
        }
    }

    @Test
    public void queryForWriteAndRead() {
        try (
            final InfluxDBContainer<?> influxDBContainer = new InfluxDBContainer<>(
                InfluxDBTestUtils.INFLUXDB_V2_TEST_IMAGE)
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
                .withOrganization(ORG)
                .withBucket(BUCKET)
                .withRetention(RETENTION)
        ) {
            influxDBContainer.start();

            try (final InfluxDBClient influxDBClient = InfluxDBTestUtils.createInfluxDBClient(influxDBContainer)) {
                try (final WriteApi writeApi = influxDBClient.makeWriteApi()) {
                    final Point point = Point
                        .measurement("temperature")
                        .addTag("location", "west")
                        .addField("value", 55.0D)
                        .time(Instant.now().toEpochMilli(), WritePrecision.MS);

                    writeApi.writePoint(point);
                }

                final String flux = String.format("from(bucket:\"%s\") |> range(start: 0)", BUCKET);

                final QueryApi queryApi = influxDBClient.getQueryApi();

                final FluxTable fluxTable = queryApi.query(flux).get(0);
                final List<FluxRecord> records = fluxTable.getRecords();
                Assertions.assertThat(records).hasSize(1);
            }
        }
    }
}
