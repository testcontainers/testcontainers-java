package org.testcontainers.containers;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class InfluxDBContainerV2WithUserTest {

    private static final String USERNAME = "new-test-user";
    private static final String PASSWORD = "new-test-password";
    private static final String ORG = "new-test-org";
    private static final String BUCKET = "new-test-bucket";
    private static final String RETENTION = "1w";
    private static final String ADMIN_TOKEN = "super-secret-token";


    private InfluxDBClient client = null;

    @ClassRule
    public static final InfluxDBContainerV2 influxDBContainerV2 =
        new InfluxDBContainerV2(InfluxDBTestImages.INFLUXDB_V2_TEST_IMAGE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .withOrganization(ORG)
            .withBucket(BUCKET)
            .withRetention(RETENTION)
            .withAdminToken(ADMIN_TOKEN);

    @Before
    public void setUp() {
        this.client = InfluxDBV2TestHelper.getInfluxDBClient(influxDBContainerV2);
    }

    @After
    public void tearDown() {
        this.client.close();
    }

    @Test
    public void getBucket() {
        assertThat(this.client, notNullValue());

        final Bucket bucket = this.client.getBucketsApi().findBucketByName(BUCKET);
        assertThat(bucket, notNullValue());

        assertThat(bucket.getName(), is(BUCKET));
    }

    @Test
    public void queryForWriteAndRead() {
        assertThat(this.client, notNullValue());

        try (final WriteApi writeApi = this.client.getWriteApi()) {

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

        final QueryApi queryApi = this.client.getQueryApi();

        final FluxTable fluxTable = queryApi.query(flux).get(0);
        final List<FluxRecord> records = fluxTable.getRecords();
        assertThat(records.size(), is(1));
    }
}
