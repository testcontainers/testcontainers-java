package org.testcontainers.images.builder.dockerfile.statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class SingleArgumentStatementTest extends AbstractStatementTest {

    SingleArgumentStatementTest(TestInfo testInfo) {
        super(testInfo);
    }

    @Test
    void simpleTest() throws Exception {
        assertStatement(new SingleArgumentStatement("TEST", "hello"));
    }

    @Test
    void multilineTest() throws Exception {
        assertStatement(new SingleArgumentStatement("TEST", "hello\nworld"));
    }
}
