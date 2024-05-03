package org.testcontainers.ext;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ScriptSplittingTest {

    @Test
    public void testStringDemarcation() {
        String script = "SELECT 'foo `bar`'; SELECT 'foo -- `bar`'; SELECT 'foo /* `bar`';";

        List<String> expected = Arrays.asList("SELECT 'foo `bar`'", "SELECT 'foo -- `bar`'", "SELECT 'foo /* `bar`'");

        splitAndCompare(script, expected);
    }

    @Test
    public void testIssue1547Case1() {
        String script =
            "create database if not exists ttt;\n" +
            "\n" +
            "use ttt;\n" +
            "\n" +
            "create table aaa\n" +
            "(\n" +
            "    id                  bigint auto_increment   primary key,\n" +
            "    end_time            datetime     null       COMMENT 'end_time',\n" +
            "    data_status         varchar(16)  not null\n" +
            ") comment 'aaa';\n" +
            "\n" +
            "create table bbb\n" +
            "(\n" +
            "    id                  bigint auto_increment   primary key\n" +
            ") comment 'bbb';";

        List<String> expected = Arrays.asList(
            "create database if not exists ttt",
            "use ttt",
            "create table aaa ( id bigint auto_increment primary key, end_time datetime null COMMENT 'end_time', data_status varchar(16) not null ) comment 'aaa'",
            "create table bbb ( id bigint auto_increment primary key ) comment 'bbb'"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testIssue1547Case2() {
        String script =
            "CREATE TABLE bar (\n" +
            "  end_time VARCHAR(255)\n" +
            ");\n" +
            "CREATE TABLE bar (\n" +
            "  end_time VARCHAR(255)\n" +
            ");";

        List<String> expected = Arrays.asList(
            "CREATE TABLE bar ( end_time VARCHAR(255) )",
            "CREATE TABLE bar ( end_time VARCHAR(255) )"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testSplittingEnquotedSemicolon() {
        String script =
            "CREATE TABLE `bar;bar` (\n" +
            "  end_time VARCHAR(255)\n" +
            ");";

        List<String> expected = Arrays.asList(
            "CREATE TABLE `bar;bar` ( end_time VARCHAR(255) )"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testUnusualSemicolonPlacement() {
        String script = "SELECT 1;;;;;SELECT 2;\n;SELECT 3\n; SELECT 4;\n SELECT 5";

        List<String> expected = Arrays.asList("SELECT 1", "SELECT 2", "SELECT 3", "SELECT 4", "SELECT 5");

        splitAndCompare(script, expected);
    }

    @Test
    public void testCommentedSemicolon() {
        String script =
            "CREATE TABLE bar (\n" + "  foo VARCHAR(255)\n" + "); \nDROP PROCEDURE IF EXISTS -- ;\n" + "    count_foo";

        List<String> expected = Arrays.asList(
            "CREATE TABLE bar ( foo VARCHAR(255) )",
            "DROP PROCEDURE IF EXISTS count_foo"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testStringEscaping() {
        String script =
            "SELECT \"a /* string literal containing comment characters like -- here\";\n" +
            "SELECT \"a 'quoting' \\\"scenario ` involving BEGIN keyword\\\" here\";\n" +
            "SELECT * from `bar`;";

        List<String> expected = Arrays.asList(
            "SELECT \"a /* string literal containing comment characters like -- here\"",
            "SELECT \"a 'quoting' \\\"scenario ` involving BEGIN keyword\\\" here\"",
            "SELECT * from `bar`"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testBlockCommentExclusion() {
        String script = "INSERT INTO bar (foo) /* ; */ VALUES ('hello world');";

        List<String> expected = Arrays.asList("INSERT INTO bar (foo) VALUES ('hello world')");

        splitAndCompare(script, expected);
    }

    @Test
    public void testBeginEndKeywordCorrectDetection() {
        String script =
            "INSERT INTO something_end (begin_with_the_token, another_field) /*end*/ VALUES /* end */ (' begin ', `end`)-- begin\n;";

        List<String> expected = Arrays.asList(
            "INSERT INTO something_end (begin_with_the_token, another_field) VALUES (' begin ', `end`)"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testCommentInStrings() {
        String script =
            "CREATE TABLE bar (foo VARCHAR(255));\n" +
            "\n" +
            "/* Insert Values */\n" +
            "INSERT INTO bar (foo) values ('--1');\n" +
            "INSERT INTO bar (foo) values ('--2');\n" +
            "INSERT INTO bar (foo) values ('/* something */');\n" +
            "/* INSERT INTO bar (foo) values (' */'); -- '*/;\n" + // purposefully broken, to see if it breaks our splitting
            "INSERT INTO bar (foo) values ('foo');";

        List<String> expected = Arrays.asList(
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
        String script =
            "CREATE TABLE bar (foo VARCHAR(255));\n" +
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

        List<String> expected = Arrays.asList(
            "CREATE TABLE bar (foo VARCHAR(255))",
            "CREATE TABLE gender (gender VARCHAR(255))",
            "CREATE TABLE ending (ending VARCHAR(255))",
            "CREATE TABLE end2 (end2 VARCHAR(255))",
            "CREATE TABLE end_2 (end2 VARCHAR(255))",
            "BEGIN\n" + "  INSERT INTO ending values ('ending');\n" + "END",
            "BEGIN\n" + "  INSERT INTO ending values ('ending');\n" + "END",
            "BEGIN--Hello\n" + "  INSERT INTO ending values ('ending');\n" + "END",
            "BEGIN\n" + "  INSERT INTO ending values ('ending');\n" + "END",
            "CREATE TABLE foo (bar VARCHAR(255))"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testProcedureBlock() {
        String script =
            "CREATE PROCEDURE count_foo()\n" +
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

        List<String> expected = Arrays.asList(
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
            "  END"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testUnclosedBlockComment() {
        String script = "SELECT 'foo `bar`'; /*";
        assertThatThrownBy(() -> doSplit(script, ScriptUtils.DEFAULT_STATEMENT_SEPARATOR))
            .isInstanceOf(ScriptUtils.ScriptParseException.class)
            .hasMessageContaining("*/");
    }

    @Test
    public void testIssue1452Case() {
        String script =
            "create table test (text VARCHAR(255));\n" +
            "\n" +
            "/* some comment */\n" +
            "insert into `test` (`text`) values ('a     b');";

        List<String> expected = Arrays.asList(
            "create table test (text VARCHAR(255))",
            "insert into `test` (`text`) values ('a     b')"
        );

        splitAndCompare(script, expected);
    }

    @Test
    public void testIfLoopBlocks() {
        String script =
            "BEGIN\n" +
            "    rec_loop: LOOP\n" +
            "        FETCH blah;\n" +
            "        IF something_wrong THEN LEAVE rec_loop; END IF;\n" +
            "        do_something_else;\n" +
            "    END LOOP;\n" +
            "END /* final comment */;";
        List<String> expected = Collections.singletonList(
            "BEGIN\n" +
            "    rec_loop: LOOP\n" +
            "        FETCH blah;\n" +
            "        IF something_wrong THEN LEAVE rec_loop; END IF;\n" +
            "        do_something_else;\n" +
            "    END LOOP;\n" +
            "END"
        );
        splitAndCompare(script, expected);
    }

    @Test
    public void testIfLoopBlocksSpecificSeparator() {
        String script =
            "BEGIN\n" +
            "    rec_loop: LOOP\n" +
            "        FETCH blah;\n" +
            "        IF something_wrong THEN LEAVE rec_loop; END IF;\n" +
            "        do_something_else;\n" +
            "    END LOOP;\n" +
            "END;\n" +
            "@\n" +
            "CALL something();\n" +
            "@\n";
        List<String> expected = Arrays.asList(
            "BEGIN\n" +
            "    rec_loop: LOOP\n" +
            "        FETCH blah;\n" +
            "        IF something_wrong THEN LEAVE rec_loop; END IF;\n" +
            "        do_something_else;\n" +
            "    END LOOP;\n" +
            "END;",
            "CALL something();"
        );
        splitAndCompare(script, expected, "@");
    }

    @Test
    public void oracleStyleBlocks() {
        String script = "BEGIN END; /\n" + "BEGIN END;";
        List<String> expected = Arrays.asList("BEGIN END;", "BEGIN END;");
        splitAndCompare(script, expected, "/");
    }

    @Test
    public void testMultiProcedureMySQLScript() {
        String script =
            "CREATE PROCEDURE doiterate(p1 INT)\n" +
            "  BEGIN\n" +
            "    label1: LOOP\n" +
            "      SET p1 = p1 + 1;\n" +
            "      IF p1 < 10 THEN\n" +
            "        ITERATE label1;\n" +
            "      END IF;\n" +
            "      LEAVE label1;\n" +
            "    END LOOP label1;\n" +
            "  END;\n" +
            "\n" +
            "CREATE PROCEDURE dowhile()\n" +
            "  BEGIN\n" +
            "    DECLARE v1 INT DEFAULT 5;\n" +
            "    WHILE v1 > 0 DO\n" +
            "      SET v1 = v1 - 1;\n" +
            "    END WHILE;\n" +
            "  END;\n" +
            "\n" +
            "CREATE PROCEDURE dorepeat(p1 INT)\n" +
            "  BEGIN\n" +
            "    SET @x = 0;\n" +
            "    REPEAT\n" +
            "      SET @x = @x + 1;\n" +
            "    UNTIL @x > p1 END REPEAT;\n" +
            "  END;";
        List<String> expected = Arrays.asList(
            "CREATE PROCEDURE doiterate(p1 INT) BEGIN\n" +
            "    label1: LOOP\n" +
            "      SET p1 = p1 + 1;\n" +
            "      IF p1 < 10 THEN\n" +
            "        ITERATE label1;\n" +
            "      END IF;\n" +
            "      LEAVE label1;\n" +
            "    END LOOP label1;\n" +
            "  END",
            "CREATE PROCEDURE dowhile() BEGIN\n" +
            "    DECLARE v1 INT DEFAULT 5;\n" +
            "    WHILE v1 > 0 DO\n" +
            "      SET v1 = v1 - 1;\n" +
            "    END WHILE;\n" +
            "  END",
            "CREATE PROCEDURE dorepeat(p1 INT) BEGIN\n" +
            "    SET @x = 0;\n" +
            "    REPEAT\n" +
            "      SET @x = @x + 1;\n" +
            "    UNTIL @x > p1 END REPEAT;\n" +
            "  END"
        );
        splitAndCompare(script, expected);
    }

    @Test
    public void testDollarQuotedStrings() {
        String script =
            "CREATE FUNCTION f ()\n" +
            "RETURNS INT\n" +
            "AS $$\n" +
            "BEGIN\n" +
            "    RETURN 1;\n" +
            "END;\n" +
            "$$ LANGUAGE plpgsql;";
        List<String> expected = Collections.singletonList(
            "CREATE FUNCTION f () RETURNS INT AS $$\n" +
            "BEGIN\n" +
            "    RETURN 1;\n" +
            "END;\n" +
            "$$ LANGUAGE plpgsql"
        );
        splitAndCompare(script, expected);
    }

    @Test
    public void testNestedDollarQuotedString() {
        //see https://www.postgresql.org/docs/current/sql-syntax-lexical.html#SQL-SYNTAX-DOLLAR-QUOTING
        String script =
            "CREATE FUNCTION f() AS $function$\n" +
            "BEGIN\n" +
            "    RETURN ($1 ~ $q$[\\t\\r\\n\\v\\\\]$q$);\n" +
            "END;\n" +
            "$function$;" +
            "create table foo ();";
        List<String> expected = Arrays.asList(
            "CREATE FUNCTION f() AS $function$\n" +
            "BEGIN\n" +
            "    RETURN ($1 ~ $q$[\\t\\r\\n\\v\\\\]$q$);\n" +
            "END;\n" +
            "$function$",
            "create table foo ()"
        );
        splitAndCompare(script, expected);
    }

    @Test
    public void testUnclosedDollarQuotedString() {
        String script = "SELECT $tag$ ..... $";
        assertThatThrownBy(() -> doSplit(script, ScriptUtils.DEFAULT_STATEMENT_SEPARATOR))
            .isInstanceOf(ScriptUtils.ScriptParseException.class)
            .hasMessageContaining("$tag$");
    }

    private void splitAndCompare(String script, List<String> expected) {
        splitAndCompare(script, expected, ScriptUtils.DEFAULT_STATEMENT_SEPARATOR);
    }

    private void splitAndCompare(String script, List<String> expected, String separator) {
        final List<String> statements = doSplit(script, separator);
        assertThat(statements).isEqualTo(expected);
    }

    private List<String> doSplit(String script, String separator) {
        final List<String> statements = new ArrayList<>();
        ScriptUtils.splitSqlScript(
            "ignored",
            script,
            separator,
            ScriptUtils.DEFAULT_COMMENT_PREFIX,
            ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
            ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER,
            statements
        );
        return statements;
    }

    @Test
    public void testIgnoreDelimitersInLiteralsAndComments() {
        assertThat(ScriptUtils.containsSqlScriptDelimiters("'@' /*@*/ \"@\" $tag$@$tag$ --@", "@")).isFalse();
    }

    @Test
    public void testContainsDelimiters() {
        assertThat(ScriptUtils.containsSqlScriptDelimiters("'@' /*@*/ @ \"@\" --@", "@")).isTrue();
    }
}
