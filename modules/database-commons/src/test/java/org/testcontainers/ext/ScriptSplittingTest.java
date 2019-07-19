package org.testcontainers.ext;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.fail;
import static org.testcontainers.ext.ScriptUtils.*;

public class ScriptSplittingTest {

    @Test
    public void testStringDemarcation() {
        String script = "SELECT 'foo `bar`'; SELECT 'foo -- `bar`'; SELECT 'foo /* `bar`';";

        List<String> expected = asList(
            "SELECT 'foo `bar`'",
            "SELECT 'foo -- `bar`'",
            "SELECT 'foo /* `bar`'"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testUnusualSemicolonPlacement() {
        String script = "SELECT 1;;;;;SELECT 2;\n;SELECT 3\n; SELECT 4;\n SELECT 5";

        List<String> expected = asList(
            "SELECT 1",
            "SELECT 2",
            "SELECT 3 ",    // TODO: Could be improved to strip trailing whitespace
            "SELECT 4",
            "SELECT 5"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testCommentedSemicolon() {
        String script = "CREATE TABLE bar (\n" +
            "  foo VARCHAR(255)\n" +
            "); \nDROP PROCEDURE IF EXISTS -- ;\n" +
            "    count_foo";

        List<String> expected = asList(
            "CREATE TABLE bar ( foo VARCHAR(255) )",
            "DROP PROCEDURE IF EXISTS count_foo"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testStringEscaping() {
        String script = "SELECT \"a /* string literal containing comment characters like -- here\";\n" +
            "SELECT \"a 'quoting' \\\"scenario ` involving BEGIN keyword\\\" here\";\n" +
            "SELECT * from `bar`;";

        List<String> expected = asList(
            "SELECT \"a /* string literal containing comment characters like -- here\"",
            "SELECT \"a 'quoting' \\\"scenario ` involving BEGIN keyword\\\" here\"",
            "SELECT * from `bar`"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testBlockCommentExclusion() {
        String script = "INSERT INTO bar (foo) /* ; */ VALUES ('hello world');";

        List<String> expected = asList(
            "INSERT INTO bar (foo) VALUES ('hello world')"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testBeginEndKeywordCorrectDetection() {
        String script = "INSERT INTO something_end (begin_with_the_token, another_field) /*end*/ VALUES /* end */ (' begin ', `end`)-- begin\n;";

        List<String> expected = asList(
            "INSERT INTO something_end (begin_with_the_token, another_field) VALUES (' begin ', `end`)"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testCommentInStrings() {
        String script = "CREATE TABLE bar (foo VARCHAR(255));\n" +
            "\n" +
            "/* Insert Values */\n" +
            "INSERT INTO bar (foo) values ('--1');\n" +
            "INSERT INTO bar (foo) values ('--2');\n" +
            "INSERT INTO bar (foo) values ('/* something */');\n" +
            "/* INSERT INTO bar (foo) values (' */'); -- '*/;\n" +      // purposefully broken, to see if it breaks our splitting
            "INSERT INTO bar (foo) values ('foo');";

        List<String> expected = asList(
            "CREATE TABLE bar (foo VARCHAR(255))",
            "INSERT INTO bar (foo) values ('--1')",
            "INSERT INTO bar (foo) values ('--2')",
            "INSERT INTO bar (foo) values ('/* something */')",
            "'); -- '*/",
            "INSERT INTO bar (foo) values ('foo')"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testMultipleBeginEndDetection() {
        String script = "CREATE TABLE bar (foo VARCHAR(255));\n" +
            "\n" +
            "CREATE TABLE gender (gender VARCHAR(255));\n" +
            "CREATE TABLE ending (ending VARCHAR(255));\n" +
            "CREATE TABLE end2 (end2 VARCHAR(255));\n" +
            "CREATE TABLE end_2 (end2 VARCHAR(255));\n" +
            "\n" +
            "BEGIN\n" +
            "  INSERT INTO ending values ('ending');\n" +
            "END;\n" +
            "\n" +
            "BEGIN\n" +
            "  INSERT INTO ending values ('ending');\n" +
            "END/*hello*/;\n" +
            "\n" +
            "BEGIN--Hello\n" +
            "  INSERT INTO ending values ('ending');\n" +
            "END;\n" +
            "\n" +
            "/*Hello*/BEGIN\n" +
            "  INSERT INTO ending values ('ending');\n" +
            "END;\n" +
            "\n" +
            "CREATE TABLE foo (bar VARCHAR(255));";

        List<String> expected = asList(
            "CREATE TABLE bar (foo VARCHAR(255))",
            "CREATE TABLE gender (gender VARCHAR(255))",
            "CREATE TABLE ending (ending VARCHAR(255))",
            "CREATE TABLE end2 (end2 VARCHAR(255))",
            "CREATE TABLE end_2 (end2 VARCHAR(255))",
            "BEGIN\n" +
                "  INSERT INTO ending values ('ending');\n" +
                "END",
            "BEGIN\n" +
                "  INSERT INTO ending values ('ending');\n" +
                "END",
            "BEGIN--Hello\n" +
                "  INSERT INTO ending values ('ending');\n" +
                "END",
            "BEGIN\n" +
                "  INSERT INTO ending values ('ending');\n" +
                "END",
            "CREATE TABLE foo (bar VARCHAR(255))"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testProcedureBlock() {
        String script = "CREATE PROCEDURE count_foo()\n" +
            "  BEGIN\n" +
            "\n" +
            "    BEGIN\n" +
            "      SELECT *\n" +
            "      FROM bar;\n" +
            "      SELECT 1\n" +
            "      FROM dual;\n" +
            "    END;\n" +
            "\n" +
            "    BEGIN\n" +
            "      select * from bar;\n" +
            "    END;\n" +
            "\n" +
            "    -- we can do comments\n" +
            "\n" +
            "    /* including block\n" +
            "       comments\n" +
            "     */\n" +
            "\n" +
            "    /* what if BEGIN appears inside a comment? */\n" +
            "\n" +
            "    select \"or what if BEGIN appears inside a literal?\";\n" +
            "\n" +
            "  END /*; */;";

        List<String> expected = asList(
            "CREATE PROCEDURE count_foo() BEGIN\n" +
                "\n" +
                "    BEGIN\n" +
                "      SELECT *\n" +
                "      FROM bar;\n" +
                "      SELECT 1\n" +
                "      FROM dual;\n" +
                "    END;\n" +
                "\n" +
                "    BEGIN\n" +
                "      select * from bar;\n" +
                "    END;\n" +
                "\n" +
                "    -- we can do comments\n" +
                "\n" +
                "    /* including block\n" +
                "       comments\n" +
                "     */\n" +
                "\n" +
                "    /* what if BEGIN appears inside a comment? */\n" +
                "\n" +
                "    select \"or what if BEGIN appears inside a literal?\";\n" +
                "\n" +
                "  END "
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testUnclosedBlockComment() {
        String script = "SELECT 'foo `bar`'; /*";

        try {
            doSplit(script);
            fail("Should have thrown!");
        } catch (ScriptUtils.ScriptParseException expected) {
            // ignore expected exception
        }
    }

    private void splitAndCompare(String script, List<String> expected) {
        final List<String> statements = doSplit(script);
        Assertions.assertThat(statements).isEqualTo(expected);
    }

    private List<String> doSplit(String script) {
        final List<String> statements = new ArrayList<>();
        ScriptUtils.splitSqlScript("ignored", script, DEFAULT_STATEMENT_SEPARATOR, DEFAULT_COMMENT_PREFIX, DEFAULT_BLOCK_COMMENT_START_DELIMITER, DEFAULT_BLOCK_COMMENT_END_DELIMITER, statements);
        return statements;
    }
}
