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

            Container.ExecResult clusterFileResult = foundationdbContainer.execInContainer("cat","/var/fdb/fdb.cluster");
            String clusterFileResultStdout = clusterFileResult.getStdout();
            System.out.println("stdout: " + clusterFileResultStdout);
            int clusterFileResultExitCode = clusterFileResult.getExitCode();
            assertTrue(clusterFileResultExitCode == 0);

            Container.ExecResult createDbResult = foundationdbContainer.execInContainer("/usr/bin/fdbcli", "--exec", "configure new single memory");
            String createDbStdout = createDbResult.getStdout();
            System.out.println("createDbStdout: " + createDbStdout);
            int createDbExitCode = createDbResult.getExitCode();
            assertTrue(createDbExitCode == 0);

             File resourcesDirectory = new File("src/test/resources/fdb.cluster");
             String absolutePath = resourcesDirectory.getAbsolutePath();
            // System.out.println("absolutePath: " + absolutePath);

            val fdb = FDBDatabaseFactory
                .instance()
                // Assumes a src/test/resources/fdb.cluster file with the following contents:
                // docker:docker@127.0.0.1:4500
                .getDatabase(absolutePath.toString());


        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private String clusterFilePath() {

        String resourceName = "fdb.cluster";

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(resourceName).getFile());
        String absolutePath = file.getAbsolutePath();

        System.out.println(absolutePath);

        assertTrue(absolutePath.endsWith("/fdb.cluster"));



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
