package org.testcontainers.images.builder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.nio.file.Paths;

@RunWith(Enclosed.class)
public class ImageFromDockerfileTest {
    @RunWith(Parameterized.class)
    public static class InvalidTarPathTests {
        @Rule
        public final ExpectedException expectedException = ExpectedException.none();

        @Parameterized.Parameters(name = "{index} - {0}")
        public static String[] getTestCases() {
            return new String[]{
                "..",
                ".",
                "../",
                "./",
                "/..",
                "/.",
                "/../",
                "/./",
                ".",
                "..",
                "aa/.",
                "aa/..",
                "bb/./",
                "bb/../"
            };
        }

        @Parameterized.Parameter
        public String tarPath;

        @Test
        public void unableToTransferFileWithDotsToDockerDaemon() {
            expectedException.expect(IllegalArgumentException.class);
            expectedException.expectMessage("Unable to store file '" +
                Paths.get("src", "test", "resources", "mappable-resource", "test-resource.txt") +
                "' to docker path '" + tarPath + "'");

            final ImageFromDockerfile imageFromDockerfile = new ImageFromDockerfile()
                .withFileFromFile(tarPath, new File("src/test/resources/mappable-resource/test-resource.txt"));
            imageFromDockerfile.resolve();
        }
    }
}
