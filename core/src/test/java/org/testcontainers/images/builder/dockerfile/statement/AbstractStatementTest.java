package org.testcontainers.images.builder.dockerfile.statement;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.TestInfo;
import org.rnorth.ducttape.Preconditions;

import java.io.InputStream;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.fail;

public abstract class AbstractStatementTest {

    private final TestInfo testInfo;

    AbstractStatementTest(TestInfo testInfo) {
        this.testInfo = testInfo;
    }

    protected void assertStatement(Statement statement) {
        String testName = testInfo.getTestMethod().get().getName();
        String[] expectedLines = new String[0];
        try {
            String path = "fixtures/statements/" + getClass().getSimpleName() + "/" + testName;
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);

            Preconditions.check("inputStream is null for path " + path, inputStream != null);

            String content = IOUtils.toString(inputStream);
            IOUtils.closeQuietly(inputStream);
            expectedLines = StringUtils.chomp(content.replaceAll("\r\n", "\n").trim()).split("\n");
        } catch (Exception e) {
            fail("can't load fixture '" + testName + "'\n" + ExceptionUtils.getStackTrace(e));
        }

        StringBuilder builder = new StringBuilder();
        statement.appendArguments(builder);
        String[] resultLines = StringUtils.chomp(builder.toString().trim()).split("\n");

        if (expectedLines.length != resultLines.length) {
            fail(
                "number of lines is not the same. Expected " + expectedLines.length + " but got " + resultLines.length
            );
        }

        if (!Arrays.equals(expectedLines, resultLines)) {
            StringBuilder failureBuilder = new StringBuilder();
            failureBuilder.append("Invalid statement!\n");

            for (int i = 0; i < expectedLines.length; i++) {
                String expectedLine = expectedLines[i];
                String actualLine = resultLines[i];

                if (!expectedLine.equals(actualLine)) {
                    failureBuilder.append("Invalid line #");
                    failureBuilder.append(i);
                    failureBuilder.append(":\n\tActual:   <");
                    failureBuilder.append(actualLine);
                    failureBuilder.append(">\n\tExpected: <");
                    failureBuilder.append(expectedLine);
                    failureBuilder.append(">\n");
                }
            }

            fail(failureBuilder.toString());
        }
    }
}
