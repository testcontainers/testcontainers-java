package org.testcontainers.images.builder.dockerfile.statement;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class KeyValuesStatementTest extends AbstractStatementTest {

    @Test
    public void multilineTest() {
        ImmutableMap<String, String> pairs = ImmutableMap
            .<String, String>builder()
            .put("line1", "1")
            .put("line2", "2")
            .put("line3", "3")
            .build();

        assertStatement(new KeyValuesStatement("TEST", pairs));
    }

    @Test
    public void keyWithSpacesTest() {
        assertStatement(new KeyValuesStatement("TEST", Collections.singletonMap("key with spaces", "1")));
    }

    @Test
    public void keyWithNewLinesTest() {
        assertStatement(new KeyValuesStatement("TEST", Collections.singletonMap("key\nwith\nnewlines", "1")));
    }

    @Test
    public void keyWithTabsTest() {
        assertStatement(new KeyValuesStatement("TEST", Collections.singletonMap("key\twith\ttab", "1")));
    }

    @Test
    public void valueIsEscapedTest() {
        ImmutableMap<String, String> pairs = ImmutableMap
            .<String, String>builder()
            .put("1", "value with spaces")
            .put("2", "value\nwith\nnewlines")
            .put("3", "value\twith\ttab")
            .build();

        assertStatement(new KeyValuesStatement("TEST", pairs));
    }
}
