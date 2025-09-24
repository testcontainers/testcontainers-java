package org.testcontainers.images.builder.dockerfile.statement;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.Collections;

class KeyValuesStatementTest extends AbstractStatementTest {

    KeyValuesStatementTest(TestInfo testInfo) {
        super(testInfo);
    }

    @Test
    void multilineTest() throws Exception {
        ImmutableMap<String, String> pairs = ImmutableMap
            .<String, String>builder()
            .put("line1", "1")
            .put("line2", "2")
            .put("line3", "3")
            .build();

        assertStatement(new KeyValuesStatement("TEST", pairs));
    }

    @Test
    void keyWithSpacesTest() throws Exception {
        assertStatement(new KeyValuesStatement("TEST", Collections.singletonMap("key with spaces", "1")));
    }

    @Test
    void keyWithNewLinesTest() throws Exception {
        assertStatement(new KeyValuesStatement("TEST", Collections.singletonMap("key\nwith\nnewlines", "1")));
    }

    @Test
    void keyWithTabsTest() throws Exception {
        assertStatement(new KeyValuesStatement("TEST", Collections.singletonMap("key\twith\ttab", "1")));
    }

    @Test
    void valueIsEscapedTest() throws Exception {
        ImmutableMap<String, String> pairs = ImmutableMap
            .<String, String>builder()
            .put("1", "value with spaces")
            .put("2", "value\nwith\nnewlines")
            .put("3", "value\twith\ttab")
            .build();

        assertStatement(new KeyValuesStatement("TEST", pairs));
    }
}
