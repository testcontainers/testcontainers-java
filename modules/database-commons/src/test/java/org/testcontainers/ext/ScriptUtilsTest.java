package org.testcontainers.ext;

import org.junit.jupiter.api.Test;
import org.testcontainers.delegate.AbstractDatabaseDelegate;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScriptUtilsTest {

    private final RecordingDatabaseDelegate delegate = new RecordingDatabaseDelegate();

    @Test
    void usesUtf8ByDefault() {
        ScriptUtils.runInitScript(delegate, "init-utf-8.sql");

        assertThat(delegate.statements).containsExactly("SELECT 'café'");
    }

    @Test
    void readsWindows1252Script() {
        ScriptUtils.runInitScript(delegate, "init-windows-1252.sql", Charset.forName("windows-1252"));

        assertThat(delegate.statements).containsExactly("SELECT 'café'");
    }

    @Test
    void readsExplicitUtf8Script() {
        ScriptUtils.runInitScript(delegate, "init-utf-8.sql", StandardCharsets.UTF_8);

        assertThat(delegate.statements).containsExactly("SELECT 'café'");
    }

    @Test
    void rejectsNullCharset() {
        assertThatThrownBy(() -> ScriptUtils.runInitScript(delegate, "init-utf-8.sql", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("charset must not be null");
    }

    private static class RecordingDatabaseDelegate extends AbstractDatabaseDelegate<Void> {

        private final List<String> statements = new ArrayList<>();

        @Override
        public void execute(
            String statement,
            String scriptPath,
            int lineNumber,
            boolean continueOnError,
            boolean ignoreFailedDrops
        ) {
            statements.add(statement);
        }

        @Override
        protected Void createNewConnection() {
            return null;
        }

        @Override
        protected void closeConnectionQuietly(Void connection) {}
    }
}
