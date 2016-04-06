package org.testcontainers.images.builder.dockerfile.statement;

import org.junit.Test;

public class MultiArgsStatementTest extends AbstractStatementTest {

    @Test
    public void simpleTest() throws Exception {
        assertStatement(new MultiArgsStatement("TEST", "a", "b", "c"));
    }

    @Test
    public void multilineTest() throws Exception {
        assertStatement(new MultiArgsStatement("TEST", "some\nmultiline\nargument"));
    }
}