package org.testcontainers.couchbase;

import com.couchbase.client.java.document.RawJsonDocument;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author ctayeb
 *         created on 18/06/2017
 */
public class CouchbaseContainerTest extends AbstractCouchbaseTest {

    private static final String ID = "toto";

    private static final String DOCUMENT = "{\"name\":\"toto\"}";

    @Test
    public void should_insert_document() {
        RawJsonDocument expected = RawJsonDocument.create(ID, DOCUMENT);
        getBucket().upsert(expected);
        RawJsonDocument result = getBucket().get(ID, RawJsonDocument.class);
        Assert.assertEquals(expected.content(), result.content());
    }

    @Test
    public void should_execute_n1ql() {
        getBucket().query(N1qlQuery.simple("INSERT INTO " + TEST_BUCKET + " (KEY, VALUE) VALUES ('" + ID + "', " + DOCUMENT + ")"));

        N1qlQueryResult query = getBucket().query(N1qlQuery.simple("SELECT * FROM " + TEST_BUCKET + " USE KEYS '" + ID + "'"));
        Assert.assertTrue(query.parseSuccess());
        Assert.assertTrue(query.finalSuccess());
        List<N1qlQueryRow> n1qlQueryRows = query.allRows();
        Assert.assertEquals(1, n1qlQueryRows.size());
        Assert.assertEquals(DOCUMENT, n1qlQueryRows.get(0).value().get(TEST_BUCKET).toString());
    }
}
