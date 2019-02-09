package org.testcontainers.junit;

import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;
import java.io.IOException;

public class CopyFileToContainerTest {
    private static String containerPath = "/tmp/mappable-resource/";
    private static String fileName = "test-resource.txt";

    @Test
    public void checkFileCopied() throws IOException, InterruptedException {
        try(
            GenericContainer container = new GenericContainer("alpine:latest")
                .withCommand("sleep","3000")
                .withCopyFileToContainer(MountableFile.forClasspathResource("/mappable-resource/"), containerPath)
        ) {
            container.start();
            String filesList = container.execInContainer("ls","/tmp/mappable-resource").getStdout();
            Assert.assertTrue(filesList.contains(fileName));
        }
    }
}
