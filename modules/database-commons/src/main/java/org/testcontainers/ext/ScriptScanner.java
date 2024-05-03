package org.testcontainers.ext;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rough lexical parser for SQL scripts.
 */
@RequiredArgsConstructor
class ScriptScanner {

    private final String resource;

    private final String script;

    private final String separator;

    private final String commentPrefix;

    private final String blockCommentStartDelimiter;

    private final String blockCommentEndDelimiter;

    private final Pattern eol = Pattern.compile("[\n\r]+");

    private final Pattern whitespace = Pattern.compile("\\s+");

    private final Pattern identifier = Pattern.compile("[a-z][a-z0-9_$]*", Pattern.CASE_INSENSITIVE);

    private final Pattern dollarQuotedStringDelimiter = Pattern.compile("\\$\\w*\\$");

    private int offset;

    @Getter
    private String currentMatch;

    private boolean matches(String substring) {
        if (script.startsWith(substring, offset)) {
            currentMatch = substring;
            offset += currentMatch.length();
            return true;
        } else {
            currentMatch = "";
            return false;
        }
    }

    private boolean matches(Pattern regexp) {
        Matcher m = regexp.matcher(script);
        m.region(offset, script.length());
        if (m.lookingAt()) {
            currentMatch = m.group();
            offset = m.end();
            return true;
        } else {
            currentMatch = "";
            return false;
        }
    }

    private boolean matchesSingleLineComment() {
        /* Matches from commentPrefix to the EOL or end of script */
        if (matches(commentPrefix)) {
            Matcher m = eol.matcher(script);
            if (m.find(offset)) {
                currentMatch = commentPrefix + script.substring(offset, m.end());
                offset = m.end();
            } else {
                currentMatch = commentPrefix + script.substring(offset);
                offset = script.length();
            }
            return true;
        }
        return false;
    }

    private boolean matchesMultilineComment() {
        /* Matches from blockCommentStartDelimiter to the next blockCommentEndDelimiter.
         * Error, if blockCommentEndDelimiter is not found. */
        if (matches(blockCommentStartDelimiter)) {
            int end = script.indexOf(blockCommentEndDelimiter, offset);
            if (end < 0) {
                throw new ScriptUtils.ScriptParseException(
                    String.format("Missing block comment end delimiter [%s].", blockCommentEndDelimiter),
                    resource
                );
            }
            end += blockCommentEndDelimiter.length();
            currentMatch = blockCommentStartDelimiter + script.substring(offset, end);
            offset = end;
            return true;
        }
        return false;
    }

    private boolean matchesQuotedString(final char quote) {
        if (script.charAt(offset) == quote) {
            boolean escaped = false;
            for (int i = offset + 1; i < script.length(); i++) {
                char c = script.charAt(i);
                if (escaped) {
                    //just skip the escaped character and drop the flag
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == quote) {
                    currentMatch = script.substring(offset, i + 1);
                    offset = i + 1;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesDollarQuotedString() {
        //Matches $<tag>$ .... $<tag>$
        if (matches(dollarQuotedStringDelimiter)) {
            String delimiter = currentMatch;
            int end = script.indexOf(delimiter, offset);
            if (end < 0) {
                throw new ScriptUtils.ScriptParseException(
                    String.format("Unclosed dollar quoted string [%s].", delimiter),
                    resource
                );
            }
            end += delimiter.length();
            currentMatch = delimiter + script.substring(offset, end);
            offset = end;
            return true;
        }
        return false;
    }

    Lexem next() {
        if (offset < script.length()) {
            if (matches(separator)) {
                return Lexem.SEPARATOR;
            } else if (matchesSingleLineComment() || matchesMultilineComment()) {
                return Lexem.COMMENT;
            } else if (matchesQuotedString('\'') || matchesQuotedString('"') || matchesQuotedString('`') || matchesDollarQuotedString()) {
                return Lexem.QUOTED_STRING;
            } else if (matches(identifier)) {
                return Lexem.IDENTIFIER;
            } else if (matches(whitespace)) {
                return Lexem.WHITESPACE;
            } else {
                currentMatch = String.valueOf(script.charAt(offset++));
                return Lexem.OTHER;
            }
        } else {
            return Lexem.EOF;
        }
    }

    enum Lexem {
        SEPARATOR,
        COMMENT,
        QUOTED_STRING,
        WHITESPACE,
        IDENTIFIER,
        OTHER,
        EOF,
    }
}
