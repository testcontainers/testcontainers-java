package org.testcontainers.containers.localstack;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.io.File;
import java.io.IOException;

public class LocalStackImageTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setupConfig() throws IOException {
        File tempDir = folder.newFolder();
        TestcontainersConfiguration instance = TestcontainersConfiguration.getInstance();
        instance.resetConfiguration(tempDir.getAbsolutePath());
    }

    @Test
    public void defaultImageTest() throws IOException {
        String defaultValue = "localstack/localstack:0.8.6";
        LocalStackContainer container = new LocalStackContainer();
        Assert.assertEquals(defaultValue, container.getDockerImageName());
    }

    @Test
    public void differentImageTest() throws IOException {
        String alt = "redis:3.0.2";
        TestcontainersConfiguration.getInstance().updateGlobalConfig("localstack.container.image", alt);
        LocalStackContainer container = new LocalStackContainer();
        Assert.assertEquals(alt, container.getDockerImageName());
    }

    @AfterClass
    public static void cleanup() {
        TestcontainersConfiguration instance = TestcontainersConfiguration.getInstance();
        instance.resetConfiguration(null);

    }
}
