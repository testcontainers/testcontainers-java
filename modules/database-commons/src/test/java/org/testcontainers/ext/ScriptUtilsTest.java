package org.testcontainers.ext;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ScriptUtilsTest {

    /*
     * Test ScriptUtils script splitting with some ugly/hard-to-split cases
     */
    @Test
    public void testSplit() throws IOException {
        final String script = Resources.toString(Resources.getResource("splittable.sql"), Charsets.UTF_8);

        final List<String> statements = new ArrayList<>();
        ScriptUtils.splitSqlScript("resourcename", script, ";", "--", "/*", "*/", statements);

        assertEquals(7, statements.size());
        assertEquals("SELECT \"a /* string literal containing comment characters like -- here\"", statements.get(2));
        assertEquals("SELECT \"a 'quoting' \\\"scenario ` involving BEGIN keyword\\\" here\"", statements.get(3));
        assertEquals("SELECT * from `bar`", statements.get(4));
        assertEquals("INSERT INTO bar (foo) VALUES ('hello world')", statements.get(6));
    }

    /*
     * Test ScriptUtils script splitting with some ugly/hard-to-split cases and linux line endings
     */
    @Test
    public void testSplitWithWidnwosLineEnding() throws IOException {
        final String script = Resources.toString(Resources.getResource("splittable.sql"), Charsets.UTF_8);
        final String scriptWithWindowsLineEndings = script.replaceAll("\n", "\r\n");
        final List<String> statements = new ArrayList<>();
        ScriptUtils.splitSqlScript("resourcename", scriptWithWindowsLineEndings, ";", "--", "/*", "*/", statements);

        assertEquals(7, statements.size());
        assertEquals("SELECT \"a /* string literal containing comment characters like -- here\"", statements.get(2));
        assertEquals("SELECT \"a 'quoting' \\\"scenario ` involving BEGIN keyword\\\" here\"", statements.get(3));
        assertEquals("SELECT * from `bar`", statements.get(4));
        assertEquals("INSERT INTO bar (foo) VALUES ('hello world')", statements.get(6));
    }
}
