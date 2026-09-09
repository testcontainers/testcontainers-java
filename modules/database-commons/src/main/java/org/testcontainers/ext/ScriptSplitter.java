package org.testcontainers.ext;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.testcontainers.ext.ScriptScanner.Lexem;

import java.util.List;

/**
 * Performs splitting of an SQL script into statements including
 * basic clean-up.
 */
@RequiredArgsConstructor
class ScriptSplitter {

    private final ScriptScanner scanner;

    private final List<String> statements;

    private final StringBuilder sb = new StringBuilder();

		// Tracks deferred whitespace between lexems in standard parsing mode
		private boolean pendingWhitespace = false;
		private boolean pendingWhitespaceHadEol = false;

    /**
     * Standard parsing:
     * 1. Remove comments
     * 2. Shrink whitespace and eols
     * 3. Split on separator
     */
    void split() {
		Lexem l;
		while ((l = scanner.next()) != Lexem.EOF) {
			switch (l) {
				case SEPARATOR:
					// statement boundary, reset any pending whitespace
					pendingWhitespace = false;
					pendingWhitespaceHadEol = false;
					flushStringBuilder();
					break;
				case COMMENT:
					// skip comments; keep pending whitespace as-is
					break;
				case WHITESPACE: {
					// Defer emitting whitespace until we know what follows.
					// This allows us to preserve a newline between adjacent quoted strings,
					// which is required by some SQL dialects (e.g. PostgreSQL) to concatenate literals.
					final String ws = scanner.getCurrentMatch();
					pendingWhitespace = true;
					pendingWhitespaceHadEol = ws.indexOf('\n') >= 0 || ws.indexOf('\r') >= 0;
					break;
				}
				case IDENTIFIER: {
					emitPendingWhitespaceIfNeeded(l);
					appendMatch();
					if ("begin".equalsIgnoreCase(scanner.getCurrentMatch())) {
						compoundStatement(false);
						flushStringBuilder();
					}
					break;
				}
				default:
					emitPendingWhitespaceIfNeeded(l);
					appendMatch();
			}
		}
		flushStringBuilder();
    }

		// helper: emits pending whitespace before the given next lexem
		private void emitPendingWhitespaceIfNeeded(Lexem nextLexem) {
			if (!pendingWhitespace) {
				return;
			}
			// Decide between ' ' and '\n'
			// Preserve a newline only when it appeared in the original whitespace
			// and it separates two quoted strings.
			final boolean prevEndsWithQuote =
				sb.length() > 0 && (sb.charAt(sb.length() - 1) == '\'' || sb.charAt(sb.length() - 1) == '"');
			if (pendingWhitespaceHadEol && prevEndsWithQuote && Lexem.QUOTED_STRING.equals(nextLexem)) {
				if (sb.length() == 0 || sb.charAt(sb.length() - 1) != '\n') {
					sb.append('\n');
				}
			} else {
				if (sb.length() == 0 || sb.charAt(sb.length() - 1) != ' ') {
					sb.append(' ');
				}
			}
			pendingWhitespace = false;
			pendingWhitespaceHadEol = false;
		}

    /**
     * Compound statement ('create procedure') mode:
     * 1. Do not remove comments
     * 2. Do not shrink whitespace
     * 3. Do not split on separators
     * 3. This mode can be recursive
     */
    private void compoundStatement(boolean recursive) {
        Lexem l;
        while ((l = scanner.next()) != Lexem.EOF) {
            appendMatch();
            if (Lexem.IDENTIFIER.equals(l)) {
                if ("begin".equalsIgnoreCase(scanner.getCurrentMatch())) {
                    compoundStatement(true);
                } else if ("end".equalsIgnoreCase(scanner.getCurrentMatch())) {
                    if (endOfBlock(recursive)) {
                        return;
                    }
                }
            }
        }
        flushStringBuilder();
    }

    private boolean endOfBlock(boolean recursive) {
        Lexem l;
        StringBuilder temporary = new StringBuilder();
        while ((l = scanner.next()) != Lexem.EOF) {
            switch (l) {
                case COMMENT:
                case WHITESPACE:
                    temporary.append(scanner.getCurrentMatch());
                    break;
                case SEPARATOR:
                    //Only whitespace and comments preceded the separator: true end of block
                    //If it's an internal block, append everything
                    if (recursive) {
                        sb.append(temporary);
                        appendMatch();
                    }
                    return true;
                default:
                    // Semicolon is not recognized as separator: this means that a custom
                    // separator is used. Still, 'END;' should be a valid end of block
                    if (";".equals(scanner.getCurrentMatch())) {
                        if (recursive) {
                            sb.append(temporary);
                        }
                        appendMatch();
                        return true;
                    }
                    sb.append(temporary);
                    appendMatch();
                    return false;
            }
        }
        return true;
    }

    private void appendMatch() {
        sb.append(scanner.getCurrentMatch());
    }

    private void flushStringBuilder() {
        final String s = sb.toString().trim();
        if (StringUtils.isNotEmpty(s)) {
            statements.add(s);
        }
        sb.setLength(0);
    }
}
