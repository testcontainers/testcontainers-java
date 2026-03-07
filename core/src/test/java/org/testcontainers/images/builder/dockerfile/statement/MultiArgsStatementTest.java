package org.testcontainers.images.builder.dockerfile.statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class MultiArgsStatementTest extends AbstractStatementTest {

    MultiArgsStatementTest(TestInfo testInfo) {
        super(testInfo);
    }

    @Test
    void simpleTest() {
        assertStatement(new MultiArgsStatement("TEST", "a", "b", "c"));
    }

    @Test
    void multilineTest() {
        assertStatement(new MultiArgsStatement("TEST", "some\nmultiline\nargument"));
    }
}
