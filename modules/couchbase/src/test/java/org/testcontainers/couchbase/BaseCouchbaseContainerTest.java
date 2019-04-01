package org.testcontainers.couchbase;

import com.couchbase.client.java.document.RawJsonDocument;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.couchbase.client.java.view.DefaultView;
import com.couchbase.client.java.view.DesignDocument;
import com.couchbase.client.java.view.View;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public abstract class BaseCouchbaseContainerTest extends AbstractCouchbaseTest {

    private static final String VIEW_NAME = "testview";
    private static final String VIEW_FUNCTION = "function (doc, meta) {\n" +
        "  emit(meta.id, null);\n" +
        "}";


    private static final String ID = "toto";

    private static final String DOCUMENT = "{\"name\":\"toto\"}";

    @Test
    public void shouldInsertDocument() {
        RawJsonDocument expected = RawJsonDocument.create(ID, DOCUMENT);
        getBucket().upsert(expected);
        RawJsonDocument result = getBucket().get(ID, RawJsonDocument.class);
        Assert.assertEquals(expected.content(), result.content());
    }

    @Test
    public void shouldExecuteN1ql() {
        getBucket().query(N1qlQuery.simple("INSERT INTO " + TEST_BUCKET + " (KEY, VALUE) VALUES ('" + ID + "', " + DOCUMENT + ")"));

        N1qlQueryResult query = getBucket().query(N1qlQuery.simple("SELECT * FROM " + TEST_BUCKET + " USE KEYS '" + ID + "'"));
        Assert.assertTrue(query.parseSuccess());
        Assert.assertTrue(query.finalSuccess());
        List<N1qlQueryRow> n1qlQueryRows = query.allRows();
        Assert.assertEquals(1, n1qlQueryRows.size());
        Assert.assertEquals(DOCUMENT, n1qlQueryRows.get(0).value().get(TEST_BUCKET).toString());
    }

    @Test
    public void shouldCreateView() {
        View view = DefaultView.create(VIEW_NAME, VIEW_FUNCTION);
        DesignDocument document = DesignDocument.create(VIEW_NAME, Lists.newArrayList(view));
        getBucket().bucketManager().insertDesignDocument(document);
        DesignDocument result = getBucket().bucketManager().getDesignDocument(VIEW_NAME);
        Assert.assertEquals(1, result.views().size());
        View resultView = result.views().get(0);
        Assert.assertEquals(VIEW_NAME, resultView.name());
    }
}
