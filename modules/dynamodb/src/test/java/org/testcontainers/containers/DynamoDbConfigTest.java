package org.testcontainers.containers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

@RunWith(Enclosed.class)
public class DynamoDbConfigTest {

    @AllArgsConstructor
    @RunWith(Parameterized.class)
    public static class Success {

        private final TestArgument argument;

        @Test
        public void shouldBeValid() {
            // given
            DynamoDbConfig config = argument.input.config;

            // when
            String command = config.toString();

            // then
            assertEquals("Config should generate correct command", argument.expected.command, command);
        }

        @Parameterized.Parameters(name = "{0}")
        public static TestArgument[] parameters() {
            return new TestArgument[] {
                TestArgument.builder()
                    .testName("validating default config")
                    .input(Input.builder().config(DynamoDbConfig.builder().build()).build())
                    .expected(Expected.builder().command("-jar DynamoDBLocal.jar -port 8000").build())
                    .build(),
                TestArgument.builder()
                    .testName("validating 'port' config")
                    .input(Input.builder().config(DynamoDbConfig.builder().port(5000).build()).build())
                    .expected(Expected.builder().command("-jar DynamoDBLocal.jar -port 5000").build())
                    .build(),
                TestArgument.builder()
                    .testName("validating 'inMemory' config")
                    .input(Input.builder().config(DynamoDbConfig.builder().inMemory(true).build()).build())
                    .expected(Expected.builder().command("-jar DynamoDBLocal.jar -port 8000 -inMemory").build())
                    .build(),
                TestArgument.builder()
                    .testName("validating 'delayTransientStatuses' config")
                    .input(Input.builder().config(DynamoDbConfig.builder().delayTransientStatuses(true).build()).build())
                    .expected(Expected.builder().command("-jar DynamoDBLocal.jar -port 8000 -delayTransientStatuses").build())
                    .build(),
                TestArgument.builder()
                    .testName("validating 'dbPath' config")
                    .input(Input.builder().config(DynamoDbConfig.builder().dbPath(".").build()).build())
                    .expected(Expected.builder().command("-jar DynamoDBLocal.jar -port 8000 -dbPath .").build())
                    .build(),
                TestArgument.builder()
                    .testName("validating 'sharedDb' config")
                    .input(Input.builder().config(DynamoDbConfig.builder().sharedDb(true).build()).build())
                    .expected(Expected.builder().command("-jar DynamoDBLocal.jar -port 8000 -sharedDb").build())
                    .build(),
                TestArgument.builder()
                    .testName("validating 'cors' config")
                    .input(Input.builder().config(DynamoDbConfig.builder().cors("*").build()).build())
                    .expected(Expected.builder().command("-jar DynamoDBLocal.jar -port 8000 -cors *").build())
                    .build(),
                TestArgument.builder()
                    .testName("validating 'optimizeDbBeforeStartup' config")
                    .input(Input.builder().config(DynamoDbConfig.builder().dbPath(".").optimizeDbBeforeStartup(true).build()).build())
                    .expected(Expected.builder().command("-jar DynamoDBLocal.jar -port 8000 -dbPath . -optimizeDbBeforeStartup").build())
                    .build()
            };
        }

        @Builder
        @AllArgsConstructor
        private static class Input {
            private final DynamoDbConfig config;
        }

        @Builder
        @AllArgsConstructor
        private static class Expected {
            private final String command;
        }

        @Builder
        @AllArgsConstructor
        private static class TestArgument {
            private final String testName;
            private final Input input;
            private final Expected expected;

            @Override
            public String toString() {
                return testName;
            }
        }
    }

    public static class Fail {

        @Test
        public void shouldThrowsExceptionWithInvalidDirectory() {
            // given
            DynamoDbConfig.DynamoDbConfigBuilder config = DynamoDbConfig.builder();

            // then
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                                                           () -> config.dbPath("invalid-path"));

            // then
            assertNotNull(exception);
            assertEquals("Directory is not valid.", exception.getMessage());
        }

        @Test
        public void shouldThrowsExceptionWithNullDirectory() {
            // given
            DynamoDbConfig.DynamoDbConfigBuilder config = DynamoDbConfig.builder();

            // when
            NullPointerException exception = assertThrows(NullPointerException.class,
                                                                 () -> config.dbPath(null));

            // then
            assertNotNull(exception);
            assertEquals("dbPath is marked non-null but is null", exception.getMessage());
        }

        @Test
        public void shouldThrowsExceptionWithConflictBetweenDbPathAndInMemory() {
            // given
            DynamoDbConfig.DynamoDbConfigBuilder config = DynamoDbConfig.builder().inMemory(true);

            // then
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                                                           () -> config.dbPath("."));

            // then
            assertNotNull(exception);
            assertEquals("You can't specify both -dbPath and -inMemory at once.", exception.getMessage());
        }

        @Test
        public void shouldThrowsExceptionWithConflictBetweenInMemoryAndDbPath() {
            // given
            DynamoDbConfig.DynamoDbConfigBuilder config = DynamoDbConfig.builder().dbPath(".");

            // then
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                                                           () -> config.inMemory(true));

            // then
            assertNotNull(exception);
            assertEquals("You can't specify both -dbPath and -inMemory at once.", exception.getMessage());
        }

        @Test
        public void shouldThrowsExceptionWithNullPort() {
            // given
            DynamoDbConfig.DynamoDbConfigBuilder config = DynamoDbConfig.builder();

            // when
            NullPointerException exception = assertThrows(NullPointerException.class,
                                                          () -> config.port(null));

            // then
            assertNotNull(exception);
            assertEquals("port is marked non-null but is null", exception.getMessage());
        }

        @Test
        public void shouldThrowsExceptionWithNullCors() {
            // given
            DynamoDbConfig.DynamoDbConfigBuilder config = DynamoDbConfig.builder();

            // when
            NullPointerException exception = assertThrows(NullPointerException.class,
                                                          () -> config.cors(null));

            // then
            assertNotNull(exception);
            assertEquals("cors is marked non-null but is null", exception.getMessage());
        }

        @Test
        public void shouldThrowsExceptionWithNegativePort() {
            // given
            DynamoDbConfig.DynamoDbConfigBuilder config = DynamoDbConfig.builder();

            // then
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                                                           () -> config.port(-10));

            // then
            assertNotNull(exception);
            assertEquals("Port cannot be a negative value.", exception.getMessage());
        }

        @Test
        public void shouldThrowsExceptionWithOptimizeDbBeforeStartupAndWithoutDbPath() {
            // given
            DynamoDbConfig.DynamoDbConfigBuilder config = DynamoDbConfig.builder().optimizeDbBeforeStartup(true);

            // then
            IllegalStateException exception = assertThrows(IllegalStateException.class, config::build);

            // then
            assertNotNull(exception);
            assertEquals("You also must specify -dbPath when you use 'optimizeDbBeforeStartup' parameter.", exception.getMessage());
        }

    }

}
