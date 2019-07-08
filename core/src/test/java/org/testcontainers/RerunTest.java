package org.testcontainers;

import org.junit.Test;

import java.io.File;

/**
 * This test only checks that our "rerun failed tests automatically" Gradle trick is working
 */
public class RerunTest {

    @Test
    public void flakyTest() throws Exception {
        File file = new File("build/test-rerun.txt");

        if (file.createNewFile()) {
            throw new IllegalStateException("Rerun me!");
        }

        file.delete();
    }
}
