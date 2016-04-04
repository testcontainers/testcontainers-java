package org.testcontainers.images.builder.dockerfile.statement;

import org.junit.Test;

public class SingleArgumentStatementTest extends AbstractStatementTest {

    @Test
    public void simpleTest() throws Exception {
        assertStatement(new SingleArgumentStatement("TEST", "hello"));
    }

    @Test
    public void multilineTest() throws Exception {
        assertStatement(new SingleArgumentStatement("TEST", "hello\nworld"));
    }
}