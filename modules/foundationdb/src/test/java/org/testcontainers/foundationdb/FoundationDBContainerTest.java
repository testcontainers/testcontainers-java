package org.testcontainers.foundationdb;

import com.apple.foundationdb.async.AsyncUtil;
import com.apple.foundationdb.record.EvaluationContext;
import com.apple.foundationdb.record.FunctionNames;
import com.apple.foundationdb.record.IsolationLevel;
import com.apple.foundationdb.record.RecordCursor;
import com.apple.foundationdb.record.RecordCursorResult;
import com.apple.foundationdb.record.RecordMetaData;
import com.apple.foundationdb.record.RecordMetaDataBuilder;
import com.apple.foundationdb.record.RecordStoreState;
import com.apple.foundationdb.record.TupleRange;
import com.apple.foundationdb.record.logging.KeyValueLogMessage;
import com.apple.foundationdb.record.logging.LogMessageKeys;
import com.apple.foundationdb.record.metadata.Index;
import com.apple.foundationdb.record.metadata.IndexAggregateFunction;
import com.apple.foundationdb.record.metadata.IndexTypes;
import com.apple.foundationdb.record.metadata.Key;
import com.apple.foundationdb.record.metadata.expressions.EmptyKeyExpression;
import com.apple.foundationdb.record.metadata.expressions.GroupingKeyExpression;
import com.apple.foundationdb.record.metadata.expressions.KeyExpression;
import com.apple.foundationdb.record.provider.foundationdb.FDBDatabaseFactory;
import com.apple.foundationdb.record.provider.foundationdb.FDBQueriedRecord;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordContext;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordStore;
import com.apple.foundationdb.record.provider.foundationdb.FDBStoredRecord;
import com.apple.foundationdb.record.provider.foundationdb.OnlineIndexer;
import com.apple.foundationdb.record.provider.foundationdb.keyspace.DirectoryLayerDirectory;
import com.apple.foundationdb.record.provider.foundationdb.keyspace.KeySpace;
import com.apple.foundationdb.record.provider.foundationdb.keyspace.KeySpaceDirectory;
import com.apple.foundationdb.record.provider.foundationdb.keyspace.KeySpacePath;
import com.apple.foundationdb.record.query.RecordQuery;
import com.apple.foundationdb.record.query.expressions.Query;
import com.apple.foundationdb.record.query.plan.plans.RecordQueryPlan;
import com.apple.foundationdb.record.sample.SampleProto;
import com.apple.foundationdb.tuple.Tuple;
import com.google.protobuf.Message;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.apple.foundationdb.record.metadata.Key.Expressions.concatenateFields;
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
    public void testStartDatabase1() {

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
            // Create a subspace using the key space API to create a subspace within
            // the cluster used by this record store. The key space API in general
            // allows the user to specify a hierarchical structure of named sub-paths.
            // Each record store can then fill in the named entries within the path
            // with values relevant to that store. If the key space includes a directory
            // layer directory, then the value supplied by the user will be replaced
            // by a short prefix supplied by the the directory layer. The results from
            // the directory layer are cached locally by the Record Layer to avoid excessive
            // database reads.
            //
            // In this case, the key space implies that there are multiple "applications"
            // that might be defined to run on the same FoundationDB cluster, and then
            // each "application" might have multiple "environments". This could be used,
            // for example, to connect to either the "prod" or "qa" environment for the same
            // application from within the same code base.
            final KeySpace keySpace = new KeySpace(
                new DirectoryLayerDirectory("application")
                    .addSubdirectory(new KeySpaceDirectory("environment", KeySpaceDirectory.KeyType.STRING))
            );

            // Create a path for the "record-layer-sample" application's demo environment.
            // Clear all existing data and then return the subspace associated with the key space path.
            final KeySpacePath path = keySpace.path("application", "record-layer-sample")
                .add("environment", "demo");

            // Clear out any data that may be in the record store.
            LOGGER.info("Clearing the Record Store ...");
            fdb.runAsync(path::deleteAllDataAsync);

            // Build the metadata. This simple approach only works for primary
            // keys and secondary indexes defined in the Protobuf message types.
            RecordMetaData rmd = RecordMetaData.build(SampleProto.getDescriptor());

            FDBRecordStore.Builder recordStoreBuilder = FDBRecordStore.newBuilder()
                .setMetaDataProvider(rmd)
                .setKeySpacePath(path);

            // Write records for Vendor and Item.
            LOGGER.info("Writing Vendor and Item record ...");
            fdb.run((FDBRecordContext cx) -> {
                FDBRecordStore store = recordStoreBuilder.copyBuilder().setContext(cx).create();
                store.saveRecord(SampleProto.Vendor.newBuilder()
                    .setVendorId(9375L)
                    .setVendorName("Acme")
                    .build());
                store.saveRecord(SampleProto.Vendor.newBuilder()
                    .setVendorId(1066L)
                    .setVendorName("Buy n Large")
                    .build());
                store.saveRecord(SampleProto.Item.newBuilder()
                    .setItemId(4836L)
                    .setItemName("GPS")
                    .setVendorId(9375L)
                    .build());
                store.saveRecord(SampleProto.Item.newBuilder()
                    .setItemId(9970L)
                    .setItemName("Personal Transport")
                    .setVendorId(1066L)
                    .build());
                store.saveRecord(SampleProto.Item.newBuilder()
                    .setItemId(8380L)
                    .setItemName("Piles of Garbage")
                    .setVendorId(1066L)
                    .build());
                return null;
            });

            // Use the primary key declared in the Vendor message type to read a
            // record.
            LOGGER.info("Reading Vendor record with primary key 9375L ...");
            SampleProto.Vendor.Builder readBuilder = fdb.run((FDBRecordContext cx) -> {
                FDBRecordStore store = recordStoreBuilder.copyBuilder().setContext(cx).open();
                return SampleProto.Vendor.newBuilder()
                    .mergeFrom(store.loadRecord(Key.Evaluated.scalar(9375L).toTuple()).getRecord());
            });
            LOGGER.info("    Result -> Id: {}, Name: {}", readBuilder.getVendorId(), readBuilder.getVendorName());

            // Using the secondary index declared in the message type, query
            // Item by vendor ID, then look up the item ID.
            LOGGER.info("Looking for item IDs with vendor ID 9375L ...");
            ArrayList<Long> ids = fdb.run((FDBRecordContext cx) -> {
                ArrayList<Long> itemIDs = new ArrayList<>();
                FDBRecordStore store = recordStoreBuilder.copyBuilder().setContext(cx).open();
                RecordQuery query = RecordQuery.newBuilder()
                    .setRecordType("Item")
                    .setFilter(Query.field("vendor_id").equalsValue(9375L))
                    .build();
                try (RecordCursor<FDBQueriedRecord<Message>> cursor = store.executeQuery(query)) {
                    RecordCursorResult<FDBQueriedRecord<Message>> result;
                    do {
                        result = cursor.getNext();
                        if (result.hasNext()) {
                            itemIDs.add(SampleProto.Item.newBuilder()
                                .mergeFrom(result.get().getRecord())
                                .getItemId());
                        }
                    } while (result.hasNext());
                }

                return itemIDs;
            });
            ids.forEach((Long res) ->
                LOGGER.info("    Result -> Vendor ID: 9375, Item ID: {}", res));

            // A kind of hand-crafted "cross-table join" (in some sense). This returns a list
            // linking the name of each vendor to the names of the products they sell.
            // Note that this query is entirely non-blocking until the end.
            // In SQL, this might look something like:
            //
            //   SELECT Vendor.name, Item.name FROM Item JOIN Vendor ON Vendor.vendor_id = Item.vid
            //
            // One difference is that the above SQL query will flatten the results out so that there
            // is exactly one returned row per item name (per vendor) where as the map returned by
            // this RecordLayer query will feature exactly one entry per vendor where the key is the
            // vendor name and the value is the vendor's items.
            //
            // Note that this query is not particularly efficient as is. To make this efficient, one
            // might consider an index on vendor name. This could scan the index to get the vendor
            // name of the Vendor record type and a second index on item by vendor ID, perhaps with
            // the item name in the value portion of the index definition. This would allow the
            // query to be satisfied with one scan of the vendor name index and another scan of the
            // item's vendor ID index (one scan per vendor).
            LOGGER.info("Grouping items by vendor ...");
            Map<String, List<String>> namesToItems = fdb.runAsync((FDBRecordContext cx) -> recordStoreBuilder.copyBuilder().setContext(cx).openAsync().thenCompose(store -> {
                // Outer plan gets all of the vendors
                RecordQueryPlan outerPlan = store.planQuery(RecordQuery.newBuilder()
                    .setRecordType("Vendor")
                    .setRequiredResults(Arrays.asList(field("vendor_id"), field("vendor_name")))
                    .build());
                // Inner plan gets all items for the given vendor ID.
                // Using "equalsParameter" does the plan once and re-uses the plan for each vendor ID.
                RecordQueryPlan innerPlan = store.planQuery(RecordQuery.newBuilder()
                    .setRecordType("Item")
                    .setRequiredResults(Collections.singletonList(field("item_name")))
                    .setFilter(Query.field("vendor_id").equalsParameter("vid"))
                    .build());
                return store.executeQuery(outerPlan)
                    // Step 1: Get all of the vendors and initiate a query for items with their vendor ID.
                    .mapPipelined(record -> {
                        SampleProto.Vendor vendor = SampleProto.Vendor.newBuilder().mergeFrom(record.getRecord()).build();
                        return innerPlan.execute(store, EvaluationContext.forBinding("vid", vendor.getVendorId()))
                            .map(innerRecord -> SampleProto.Item.newBuilder().mergeFrom(innerRecord.getRecord()).getItemName())
                            .asList()
                            .thenApply(list -> Pair.of(vendor.getVendorName(), list));
                    }, 10)
                    .asList()

                    // Step 2: Collect the results of the subqueries and package them as a map.
                    .thenApply((List<Pair<String, List<String>>> list) ->
                        list.stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue))
                    );
            })).join();

            namesToItems.forEach((String name, List<String> items) -> LOGGER.info("    Result -> Vendor Name: {}, Item names: {}", name, items));

            // Richer indexes:

            // To build richer primary keys or secondary indexes (than those definable in the protobuf
            // message types), you need to use the more verbose and powerful RecordMetaDataBuilder.
            RecordMetaDataBuilder rmdBuilder = RecordMetaData.newBuilder().setRecords(SampleProto.getDescriptor());

            // Order customers by last name, then first name, then their ID if otherwise equal.
            // NOTE: This operation is dangerous if you have existing data! Existing records are *not*
            // automatically migrated.
            rmdBuilder.getRecordType("Customer").setPrimaryKey(
                concatenateFields("last_name", "first_name", "customer_id"));

            // Add a global count index. Most record stores should probably add this index as it allows
            // the database to make intelligent decisions based on the current size of the record store.
            rmdBuilder.addUniversalIndex(new Index("globalCount", new GroupingKeyExpression(EmptyKeyExpression.EMPTY, 0), IndexTypes.COUNT));

            // Add a FanType.FanOut secondary index for email_address, so that
            // each value for email_address generates its own key in the index.
            rmdBuilder.addIndex("Customer", new Index("email_address",
                field("email_address", KeyExpression.FanType.FanOut),
                IndexTypes.VALUE));

            // Add a FanType.Concatenate secondary index for preference_tag, so
            // that all values for preference_tag generate a single key in the index.
            rmdBuilder.addIndex("Customer", new Index("preference_tag",
                field("preference_tag", KeyExpression.FanType.Concatenate),
                IndexTypes.VALUE));

            // Add an index on the count of each preference tag. This allows us to
            // quickly get the number of users for each preference tag. The key
            // provided will create a separate "count" field for each value of the
            // preference_tag field and keep track of the number of customer
            // records with each value.
            rmdBuilder.addIndex("Customer", new Index("preference_tag_count",
                new GroupingKeyExpression(field("preference_tag", KeyExpression.FanType.FanOut), 0),
                IndexTypes.COUNT));

            // Add a nested secondary index for order such that each value for
            // quantity in Order generates a single key in the index.
            rmdBuilder.addIndex("Customer", new Index("order",
                field("order", KeyExpression.FanType.FanOut).nest("quantity"),
                IndexTypes.VALUE));

            // Add an index on the sum of the quantity of each item in each
            // order. This can be used to know how many of each item have been ordered across
            // all customers. The grouping key here is a little hairy, but it
            // specifies that the "item_id" column should be used as a grouping key
            // and the quantity used as the sum value, so it will keep track of the
            // quantity ordered of each item.
            rmdBuilder.addIndex("Customer", new Index("item_quantity_sum",
                new GroupingKeyExpression(field("order", KeyExpression.FanType.FanOut)
                    .nest(concatenateFields("item_id", "quantity")), 1),
                IndexTypes.SUM
            ));

            // Rebuild the metadata for the newly added indexes before reading or
            // writing more data.
            RecordMetaData rmd2 = rmdBuilder.getRecordMetaData();
            recordStoreBuilder.setMetaDataProvider(rmd2);

            // Calling "open" on an existing record store with new meta-data will
            // create the index and place them in a "write-only" mode that means that
            // they cannot yet be used for queries. (In particular, the query planner
            // will ignore this index and any attempt to read from the index will
            // throw an error.) To enable querying, one must invoke the online index
            // builder. This will scan through the record store across multiple
            // transactions and populate the new indexes with data from the existing
            // entries. During the build job, the record store remains available for
            // reading and writing, but there may be additional conflicts if the index
            // build job and normal operations happen to mutate the same records.
            RecordStoreState storeState = fdb.run(cx -> {
                FDBRecordStore store = recordStoreBuilder.copyBuilder().setContext(cx).open();
                return store.getRecordStoreState();
            });
            LOGGER.info("Running index builds of new indexes:");
            // Build all of the indexes in parallel by firing off a future for each and
            // then wait for all of them.
            AsyncUtil.whenAll(storeState.getWriteOnlyIndexNames().stream()
                .map(indexName -> {
                    // Build this index. It will begin the background job and return a future
                    // that will complete when the index is ready for querying.
                    OnlineIndexer indexBuilder = OnlineIndexer.newBuilder().setDatabase(fdb).setRecordStoreBuilder(recordStoreBuilder).setIndex(indexName).build();
                    return indexBuilder.buildIndexAsync()
                        .thenRun(() -> LOGGER.info("  Index build of {} is complete.", indexName))
                        .whenComplete((vignore, eignore) -> indexBuilder.close());
                })
                .collect(Collectors.toList())
            ).join();

            // Write larger records for Customer (and Order).
            LOGGER.info("Adding records with new secondary indexes ...");
            fdb.run((FDBRecordContext cx) -> {
                FDBRecordStore store = recordStoreBuilder.copyBuilder().setContext(cx).open();

                store.saveRecord(SampleProto.Customer.newBuilder()
                    .setCustomerId(9264L)
                    .setFirstName("John")
                    .setLastName("Smith")
                    .addEmailAddress("jsmith@example.com")
                    .addEmailAddress("john_smith@example.com")
                    .addPreferenceTag("books")
                    .addPreferenceTag("movies")
                    .addOrder(
                        SampleProto.Order.newBuilder()
                            .setOrderId(3875L)
                            .setItemId(9374L)
                            .setQuantity(2)
                    )
                    .addOrder(
                        SampleProto.Order.newBuilder()
                            .setOrderId(4828L)
                            .setItemId(2740L)
                            .setQuantity(1)
                    )
                    .setPhoneNumber("(703) 555-8255")
                    .build());

                store.saveRecord(SampleProto.Customer.newBuilder()
                    .setCustomerId(8365L)
                    .setFirstName("Jane")
                    .setLastName("Doe")
                    .addEmailAddress("jdoe@example.com")
                    .addEmailAddress("jane_doe@example.com")
                    .addPreferenceTag("games")
                    .addPreferenceTag("lawn")
                    .addPreferenceTag("books")
                    .addOrder(
                        SampleProto.Order.newBuilder()
                            .setOrderId(9280L)
                            .setItemId(2740L)
                            .setQuantity(3)
                    )
                    .setPhoneNumber("(408) 555-0248")
                    .build());

                return null;
            });

            // Get the record count. This uses the global count index to get the
            // full number of records in the store.
            Long recordCount = fdb.runAsync((FDBRecordContext cx) ->
                recordStoreBuilder.copyBuilder().setContext(cx).openAsync()
                    .thenCompose(FDBRecordStore::getSnapshotRecordCount)
            ).join();
            LOGGER.info("Store contains {} records.", recordCount);

            // Query all records with the first name "Jane".
            // Performs a full scan of the primary key index.
            LOGGER.info("Retrieving all customers with first name \"Jane\"...");
            List<String> names = fdb.run((FDBRecordContext cx) -> {
                RecordQuery query = RecordQuery.newBuilder()
                    .setRecordType("Customer")
                    .setFilter(Query.field("first_name").equalsValue("Jane"))
                    .build();
                return readNames(recordStoreBuilder, cx, query);
            });
            names.forEach((String res) -> LOGGER.info("    Result -> {}", res));

            // Query all records with last name "Doe".
            // Scans only the customers from the primary key index.
            LOGGER.info("Retrieving all customers with last name \"Doe\"...");
            names = fdb.run((FDBRecordContext cx) -> {
                RecordQuery query = RecordQuery.newBuilder()
                    .setRecordType("Customer")
                    .setFilter(Query.field("last_name").equalsValue("Doe"))
                    .build();
                return readNames(recordStoreBuilder, cx, query);
            });
            names.forEach((String res) -> LOGGER.info("    Result -> {}", res));

            // Query all records with first_name "Jane" and last_name "Doe"
            // Scans only the customers from the primary key index.
            LOGGER.info("Retrieving all customers with name \"Jane Doe\"...");
            names = fdb.run((FDBRecordContext cx) -> {
                RecordQuery query = RecordQuery.newBuilder()
                    .setRecordType("Customer")
                    .setFilter(Query.and(
                        Query.field("first_name").equalsValue("Jane"),
                        Query.field("last_name").equalsValue("Doe"))
                    )
                    .build();
                return readNames(recordStoreBuilder, cx, query);
            });
            names.forEach((String res) -> LOGGER.info("    Result -> {}", res));

            // Query all records with an email address beginning with "john".
            // Uses FanType.FanOut secondary index.
            LOGGER.info("Retrieving all customers with an email address beginning with \"john\"...");
            Map<String, List<String>> addresses = fdb.run((FDBRecordContext cx) -> {
                FDBRecordStore store = recordStoreBuilder.copyBuilder().setContext(cx).open();
                RecordQuery query = RecordQuery.newBuilder()
                    .setRecordType("Customer")
                    .setFilter(Query.field("email_address").oneOfThem().startsWith("john"))
                    .build();
                Map<String, List<String>> addressMap = new HashMap<>();
                try (RecordCursor<FDBQueriedRecord<Message>> cursor = store.executeQuery(query)) {
                    cursor.forEach((FDBQueriedRecord<Message> record) -> {
                        SampleProto.Customer.Builder builder = SampleProto.Customer.newBuilder().mergeFrom(record.getRecord());
                        addressMap.put(builder.getFirstName() + " " + builder.getLastName(),
                            builder.getEmailAddressList());
                    });
                }
                return addressMap;
            });
            addresses.forEach((String k, List<String> vals) ->
                LOGGER.info("    Result -> {} with emails {}", k, vals));

            // Query all records with preference_tags "books" and "movies".
            // Uses FanType.Concatenate secondary index.
            LOGGER.info("Retrieving all customers with preference tags \"books\" and \"movies\"...");
            names = fdb.run((FDBRecordContext cx) -> {
                RecordQuery query = RecordQuery.newBuilder()
                    .setRecordType("Customer")
                    .setFilter(Query.and(
                        Query.field("preference_tag").oneOfThem().equalsValue("books"),
                        Query.field("preference_tag").oneOfThem().equalsValue("movies"))
                    )
                    .build();
                return readNames(recordStoreBuilder, cx, query);
            });
            names.forEach((String res) -> LOGGER.info("    Result -> {}", res));

            // Get the number of customers who have "books" listed as one of their preference tags
            Long bookPreferenceCount = fdb.runAsync((FDBRecordContext cx) -> recordStoreBuilder.copyBuilder().setContext(cx).openAsync().thenCompose(store -> {
                Index index = store.getRecordMetaData().getIndex("preference_tag_count");
                IndexAggregateFunction function = new IndexAggregateFunction(FunctionNames.COUNT, index.getRootExpression(), index.getName());
                return store.evaluateAggregateFunction(Collections.singletonList("Customer"), function, Key.Evaluated.scalar("books"), IsolationLevel.SERIALIZABLE)
                    .thenApply(tuple -> tuple.getLong(0));
            })).join();
            LOGGER.info("Number of customers with the \"books\" preference tag: {}", bookPreferenceCount);

            // Query all customers with an order of quantity greater than 2.
            // Uses nested secondary index.
            LOGGER.info("Retrieving all customers with an order of quantity greater than 2 ...");
            names = fdb.run((FDBRecordContext cx) -> {
                RecordQuery query = RecordQuery.newBuilder()
                    .setRecordType("Customer")
                    .setFilter(Query.field("order").oneOfThem().matches(Query.field("quantity").greaterThan(2)))
                    .build();
                return readNames(recordStoreBuilder, cx, query);
            });
            names.forEach((String res) -> LOGGER.info("    Result -> {}", res));

            // Get the sum of the quantity of items ordered for item ID 2740.
            // Using the index, it can determine this by reading a single
            // key in the database.
            Long itemQuantitySum = fdb.runAsync((FDBRecordContext cx) -> recordStoreBuilder.copyBuilder().setContext(cx).openAsync().thenCompose(store -> {
                Index index = store.getRecordMetaData().getIndex("item_quantity_sum");
                IndexAggregateFunction function = new IndexAggregateFunction(FunctionNames.SUM, index.getRootExpression(), index.getName());
                return store.evaluateAggregateFunction(Collections.singletonList("Customer"), function, Key.Evaluated.scalar(2740L), IsolationLevel.SERIALIZABLE)
                    .thenApply(tuple -> tuple.getLong(0));
            })).join();
            LOGGER.info("Total quantity ordered of item 2740L: {}", itemQuantitySum);

            // Get the sum of the quantity of all items ordered.
            // Using the index, it will do a scan that will hit one key
            // for each unique item id with a single range scan.
            Long allItemsQuantitySum = fdb.runAsync((FDBRecordContext cx) -> recordStoreBuilder.copyBuilder().setContext(cx).openAsync().thenCompose(store -> {
                Index index = store.getRecordMetaData().getIndex("item_quantity_sum");
                IndexAggregateFunction function = new IndexAggregateFunction(FunctionNames.SUM, index.getRootExpression(), index.getName());
                return store.evaluateAggregateFunction(Collections.singletonList("Customer"), function, TupleRange.ALL, IsolationLevel.SERIALIZABLE)
                    .thenApply(tuple -> tuple.getLong(0));
            })).join();
            LOGGER.info("Total quantity ordered of all items: {}", allItemsQuantitySum);
            /* End: FoundationDB open source project source code */

        } catch (InterruptedException e) {
            assertTrue("Error: " + e, false);
        } catch (IOException e) {
            assertTrue("Error: " + e, false);
        }
    }

    @Test
    public void testStartDatabase2() {

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

        } catch (InterruptedException e) {
            assertTrue("Error: " + e, false);
        } catch (IOException e) {
            assertTrue("Error: " + e, false);
        }
    }
}
