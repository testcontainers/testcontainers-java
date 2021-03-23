package org.testcontainers.foundationdb;

import com.apple.foundationdb.record.provider.foundationdb.FDBDatabaseFactory;
import lombok.val;
import org.junit.Test;
import org.testcontainers.containers.Container;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class FoundationDBContainerTest {

    @Test
    public void supportsFoundationDB_6_28() {
        try (
            final FoundationDBContainer foundationdbContainer = new FoundationDBContainer()
        ) {
            foundationdbContainer.start();
        }
    }

    @Test
    public void addDatabase() {


        try (
            final FoundationDBContainer foundationdbContainer = new FoundationDBContainer()
        ) {
            foundationdbContainer.start();

            System.out.println("FoundationDB started");
            System.out.println("Cluster file path: " + "src/test/resources/fdb.cluster");

            Container.ExecResult lsResult = foundationdbContainer.execInContainer("ls", "-al", "/");
            String stdout = lsResult.getStdout();
            System.out.println("stdout: " + stdout);
            int exitCode = lsResult.getExitCode();
            assertTrue(exitCode == 0);

            Container.ExecResult createDbResult = foundationdbContainer.execInContainer("/usr/bin/fdbcli", "--exec", "configure new single memory");
            String createDbStdout = createDbResult.getStdout();
            System.out.println("createDbStdout: " + createDbStdout);
            int createDbExitCode = createDbResult.getExitCode();
            assertTrue(createDbExitCode == 0);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private String clusterFilePath() {

        return "src/test/resources/fdb.cluster";
//
//        String resourceName = "fdb.cluster";
//
//        ClassLoader classLoader = getClass().getClassLoader();
//        File file = new File(classLoader.getResource(resourceName).getFile());
//        String absolutePath = file.getAbsolutePath();
//
//        System.out.println(absolutePath);
//        return absolutePath;
    }





}
