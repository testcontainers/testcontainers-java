package org.testcontainers.images.builder.dockerfile.statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class RawStatementTest extends AbstractStatementTest {

    RawStatementTest(TestInfo testInfo) {
        super(testInfo);
    }

    @Test
    void simpleTest() throws Exception {
        assertStatement(new RawStatement("TEST", "value\nas\t\\\nis"));
    }
}
