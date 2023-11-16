package org.testcontainers.ext;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class ScriptScannerTest {
    @Test
    public void testHugeStringLiteral() {
        String script = "/* a comment */    \"" +  StringUtils.repeat('~', 10000) + "\";";
        ScriptScanner scanner = scanner(script);
        assertThat(scanner.next()).isEqualTo(ScriptScanner.Lexem.COMMENT);
        assertThat(scanner.next()).isEqualTo(ScriptScanner.Lexem.WHITESPACE);
        assertThat(scanner.next()).isEqualTo(ScriptScanner.Lexem.QUOTED_STRING);
        assertThat(scanner.getCurrentMatch()).matches(Pattern.compile("\"~+\""));
    }

    @Test
    public void testPgIdentifierWithDollarSigns() {
        ScriptScanner scanner = scanner("this$is$a$valid$postgreSQL$identifier  " +
            "$a$While this is a quoted string$a$$ --just followed by a dollar sign");
        assertThat(scanner.next()).isEqualTo(ScriptScanner.Lexem.IDENTIFIER);
        assertThat(scanner.next()).isEqualTo(ScriptScanner.Lexem.WHITESPACE);
        assertThat(scanner.next()).isEqualTo(ScriptScanner.Lexem.QUOTED_STRING);
        assertThat(scanner.next()).isEqualTo(ScriptScanner.Lexem.OTHER);
    }

    @Test
    public void testQuotedLiterals(){
        ScriptScanner scanner = scanner("'this \\'is a literal' \"this \\\" is a literal\"");
        assertThat(scanner.next()).isEqualTo(ScriptScanner.Lexem.QUOTED_STRING);
        assertThat(scanner.getCurrentMatch()).isEqualTo("'this \\'is a literal'");
        assertThat(scanner.next()).isEqualTo(ScriptScanner.Lexem.WHITESPACE);
        assertThat(scanner.next()).isEqualTo(ScriptScanner.Lexem.QUOTED_STRING);
        assertThat(scanner.getCurrentMatch()).isEqualTo("\"this \\\" is a literal\"");
    }

    private static ScriptScanner scanner(String script) {
        return new ScriptScanner("dummy",
            script,
            ScriptUtils.DEFAULT_STATEMENT_SEPARATOR,
            ScriptUtils.DEFAULT_COMMENT_PREFIX,
            ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
            ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
    }
}
