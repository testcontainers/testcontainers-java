package org.testcontainers.foundationdb;

import com.apple.foundationdb.record.RecordCursor;
import com.apple.foundationdb.record.RecordCursorResult;
import com.apple.foundationdb.record.RecordMetaData;
import com.apple.foundationdb.record.RecordMetaDataBuilder;
import com.apple.foundationdb.record.logging.KeyValueLogMessage;
import com.apple.foundationdb.record.logging.LogMessageKeys;
import com.apple.foundationdb.record.provider.foundationdb.FDBDatabaseFactory;
import com.apple.foundationdb.record.provider.foundationdb.FDBQueriedRecord;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordContext;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordStore;
import com.apple.foundationdb.record.provider.foundationdb.FDBStoredRecord;
import com.apple.foundationdb.record.provider.foundationdb.keyspace.KeySpace;
import com.apple.foundationdb.record.provider.foundationdb.keyspace.KeySpaceDirectory;
import com.apple.foundationdb.record.provider.foundationdb.keyspace.KeySpacePath;
import com.apple.foundationdb.record.query.RecordQuery;
import com.apple.foundationdb.record.query.plan.plans.RecordQueryPlan;
import com.apple.foundationdb.record.sample.SampleProto;
import com.apple.foundationdb.tuple.Tuple;
import com.google.protobuf.Message;
import lombok.val;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.apple.foundationdb.record.metadata.Key.Expressions.field;
import static org.junit.Assert.assertTrue;

/**
 * FoundationDB Container tests.
 *
 * Disclaimer: Some source code comes the FoundationDB open source project. The code is marked
 * with clear start and end markers.
 *
 */
public class FoundationDBContainerTest {

    /* Start: FoundationDB open source project source code */
    private static final Logger LOGGER = LoggerFactory.getLogger(FoundationDBContainerTest.class);

    public static List<String> readNames(FDBRecordStore.Builder recordStoreBuilder, FDBRecordContext cx, RecordQuery query) {
        List<String> names = new ArrayList<>();
        FDBRecordStore store = recordStoreBuilder.copyBuilder().setContext(cx).open();
        RecordQueryPlan plan = store.planQuery(query);
        LOGGER.info(KeyValueLogMessage.of("Query planned", LogMessageKeys.PLAN, plan));  // The plan string works like a basic "explain" function
        try (RecordCursor<FDBQueriedRecord<Message>> cursor = store.executeQuery(plan)) {
            RecordCursorResult<FDBQueriedRecord<Message>> result;
            do {
                result = cursor.getNext();
                if (result.hasNext()) {
                    SampleProto.Customer.Builder builder = SampleProto.Customer.newBuilder().mergeFrom(result.get().getRecord());
                    names.add(builder.getFirstName() + " " + builder.getLastName());
                }
            } while (result.hasNext());
        }
        return names;
    }

    private enum FlowerType {
        ROSE,
        TULIP,
        LILY,
    }

    private static RecordLayerDemoProto.Flower buildFlower(FlowerType type, RecordLayerDemoProto.Color color) {
        return RecordLayerDemoProto.Flower.newBuilder()
            .setType(type.name())
            .setColor(color)
            .build();
    }
    /* End: FoundationDB open source project source code */

    @Test
    public void supportsFoundationDB_6_28() {
        try (
            final FoundationDBContainer foundationdbContainer = new FoundationDBContainer()
        ) {
            foundationdbContainer.start();
        }
    }

    @Test
    public void testStartDatabase() {

        try (
            final FoundationDBContainer foundationdbContainer = new FoundationDBContainer()
        ) {
            foundationdbContainer.start();

            Container.ExecResult createDbResult = foundationdbContainer.execInContainer("/usr/bin/fdbcli", "--exec", "configure new single memory");
            int createDbExitCode = createDbResult.getExitCode();
            assertTrue(createDbExitCode == 0);

            val fdb = FDBDatabaseFactory
                .instance()
                .getDatabase();

            /* Start: FoundationDB open source project source code */
            // Define the keyspace for our application
            KeySpace keySpace = new KeySpace(new KeySpaceDirectory("record-layer-demo", KeySpaceDirectory.KeyType.STRING, "record-layer-demo"));
            // Get the path where our record store will be rooted
            KeySpacePath path = keySpace.path("record-layer-demo");

            RecordMetaDataBuilder metaDataBuilder = RecordMetaData.newBuilder()
                .setRecords(RecordLayerDemoProto.getDescriptor());

            metaDataBuilder.getRecordType("Order")
                .setPrimaryKey(field("order_id"));

            Function<FDBRecordContext, FDBRecordStore> recordStoreProvider = context -> FDBRecordStore.newBuilder()
                .setMetaDataProvider(metaDataBuilder)
                .setContext(context)
                .setKeySpacePath(path)
                .createOrOpen();

            fdb.run(context -> {
                FDBRecordStore recordStore = recordStoreProvider.apply(context);

                recordStore.saveRecord(RecordLayerDemoProto.Order.newBuilder()
                    .setOrderId(1)
                    .setPrice(123)
                    .setFlower(buildFlower(FlowerType.ROSE, RecordLayerDemoProto.Color.RED))
                    .build());
                recordStore.saveRecord(RecordLayerDemoProto.Order.newBuilder()
                    .setOrderId(23)
                    .setPrice(34)
                    .setFlower(buildFlower(FlowerType.ROSE, RecordLayerDemoProto.Color.PINK))
                    .build());
                recordStore.saveRecord(RecordLayerDemoProto.Order.newBuilder()
                    .setOrderId(3)
                    .setPrice(55)
                    .setFlower(buildFlower(FlowerType.TULIP, RecordLayerDemoProto.Color.YELLOW))
                    .build());
                recordStore.saveRecord(RecordLayerDemoProto.Order.newBuilder()
                    .setOrderId(100)
                    .setPrice(9)
                    .setFlower(buildFlower(FlowerType.LILY, RecordLayerDemoProto.Color.RED))
                    .build());

                return null;
            });

            FDBStoredRecord<Message> storedRecord = fdb.run(context ->
                // load the record
                recordStoreProvider.apply(context).loadRecord(Tuple.from(1))
            );
            assert storedRecord != null;

            // a record that doesn't exist is null
            FDBStoredRecord<Message> shouldNotExist = fdb.run(context ->
                recordStoreProvider.apply(context).loadRecord(Tuple.from(99999))
            );
            assert shouldNotExist == null;
            /* End: FoundationDB open source project source code */

        } catch (Exception e) {
            assertTrue("Error: " + e, false);
        }
    }
}
