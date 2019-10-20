package generic;

import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import java.io.*;

public class CopyFilesToAndFromContainersTest {

    @Test
    public void testCopyToContainersTest() throws IOException {
        File tempFile = createTempFile();
        MountableFile mountableFile = MountableFile.forHostPath(tempFile.getPath());

        // copyToContainer {

        GenericContainer container = new GenericContainer("alpine:3.2")
            .withCopyFileToContainer(mountableFile, "/tmp/temp-in-container.tmp");
        container.start();

        container.copyFileFromContainer("/tmp/temp-in-container.tmp",
            "/tmp/temp-in-host.tmp");

        // }

        Assert.assertEquals(new RandomAccessFile(tempFile, "r").getChannel().size(),
            new RandomAccessFile("/tmp/temp-in-host.tmp", "r").getChannel().size());
    }

    private File createTempFile() throws IOException {
        File tempFile = File.createTempFile("temp", ".tmp");
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(tempFile));
        bufferedWriter.write("testing copy to docker");
        bufferedWriter.flush();

        return tempFile;
    }
}
