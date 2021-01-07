package org.testcontainers.containers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import java.time.Instant;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class InfluxDBContainerWithUserTest {

    private static final String BUCKET = "new-test-bucket";
    private static final String USER = "new-test-user";
    private static final String PASSWORD = "new-test-password";
    private static final String ORG = "new-test-org";

    private InfluxDBClient client = null;

    @ClassRule
    public static final InfluxDBContainer<?> influxDBContainer = InfluxDBContainer
        .createWithDefaultTag()
        .withBucket(BUCKET)
        .withUsername(USER)
        .withPassword(PASSWORD)
        .withOrganization(ORG);

    @Before
    public void setUp() {
        this.client = influxDBContainer.getNewInfluxDB();
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
        try (final WriteApi writeApi = this.client.getWriteApi()) {

            //
            // Write by Data Point
            //
            final Point point = Point.measurement("temperature")
                .addTag("location", "west")
                .addField("value", 55D)
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
