package org.testcontainers.containers.localstack;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.function.Consumer;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNotEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@RunWith(Enclosed.class)
public class LegacyModeTest {

    @RunWith(Parameterized.class)
    @AllArgsConstructor
    public static class Off {
        private final String version;
        private final LocalStackContainer localstack;

        @Parameterized.Parameters(name = "{0}")
        public static Iterable<Object[]> constructors() {
            return Arrays.asList(new Object[][]{
                {"default constructor", new LocalStackContainer()},
                {"latest", new LocalStackContainer("latest")},
                {"0.11.1", new LocalStackContainer("0.11.1")},
                {"0.7.0 with legacy = off", new LocalStackContainer("0.7.0", false)}
            });
        }

        @Test
        public void samePortIsExposedForAllServices() {
            localstack.withServices(S3, SQS);
            localstack.start();

            assertTrue("A single port is exposed", localstack.getExposedPorts().size() == 1);
            assertEquals(
                "Endpoint overrides are different",
                localstack.getEndpointOverride(S3).toString(),
                localstack.getEndpointOverride(SQS).toString());
            assertEquals(
                "Endpoint configuration have different endpoints",
                localstack.getEndpointConfiguration(S3).getServiceEndpoint(),
                localstack.getEndpointConfiguration(SQS).getServiceEndpoint());
        }

        @After
        public void cleanup() {
            if (localstack != null) localstack.stop();
        }
    }

    @RunWith(Parameterized.class)
    @AllArgsConstructor
    public static class On {
        private final String version;
        private final LocalStackContainer localstack;

        @BeforeClass
        public static void createCustomTag() {
            run("docker pull localstack/localstack:latest");
            run("docker tag localstack/localstack:latest localstack/localstack:custom");
        }

        @Parameterized.Parameters(name = "{0}")
        public static Iterable<Object[]> constructors() {
            return Arrays.asList(new Object[][]{
                {"0.10.7", new LocalStackContainer("0.10.7")},
                {"custom", new LocalStackContainer("custom")},
                {"0.11.1 with legacy = on", new LocalStackContainer("0.11.1", true)}
            });
        }

        @Test
        public void differentPortsAreExposed() {
            localstack.withServices(S3, SQS);
            localstack.start();

            assertTrue("Multiple ports are exposed", localstack.getExposedPorts().size() > 1);
            assertNotEquals(
                "Endpoint overrides are different",
                localstack.getEndpointOverride(S3).toString(),
                localstack.getEndpointOverride(SQS).toString());
            assertNotEquals(
                "Endpoint configuration have different endpoints",
                localstack.getEndpointConfiguration(S3).getServiceEndpoint(),
                localstack.getEndpointConfiguration(SQS).getServiceEndpoint());
        }

        @After
        public void cleanup() {
            if (localstack != null) localstack.stop();
        }
    }

    @SneakyThrows
    private static void run(String command) {
        Process process = Runtime.getRuntime().exec(command);
        join(process.getInputStream(), System.out::println);
        join(process.getErrorStream(), System.err::println);
        process.waitFor();
        if (process.exitValue() != 0)
            throw new RuntimeException("Failed to execute " + command);
    }

    private static void join(InputStream stream, Consumer<String> logger) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            logger.accept(line);
        }
    }

}
