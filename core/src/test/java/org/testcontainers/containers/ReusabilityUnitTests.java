package org.testcontainers.containers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.command.CreateContainerCmdImpl;
import com.github.dockerjava.core.command.InspectContainerCmdImpl;
import com.github.dockerjava.core.command.ListContainersCmdImpl;
import com.github.dockerjava.core.command.StartContainerCmdImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.TestImages;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.utility.MockTestcontainersConfigurationExtension;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ReusabilityUnitTests {

    @Nested
    class CanBeReusedTest {

        public static Object[][] data() {
            return new Object[][] {
                { "generic", new GenericContainer<>(TestImages.TINY_IMAGE), true },
                { "anonymous generic", new GenericContainer(TestImages.TINY_IMAGE) {}, true },
                { "custom", new CustomContainer(), true },
                { "anonymous custom", new CustomContainer() {}, true },
                { "custom with containerIsCreated", new CustomContainerWithContainerIsCreated(), false },
            };
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("data")
        void shouldBeReusable(String name, GenericContainer container, boolean reusable) {
            if (reusable) {
                assertThat(container.canBeReused()).as("Is reusable").isTrue();
            } else {
                assertThat(container.canBeReused()).as("Is not reusable").isFalse();
            }
        }

        static class CustomContainer extends GenericContainer<CustomContainer> {

            CustomContainer() {
                super(TestImages.TINY_IMAGE);
            }
        }

        static class CustomContainerWithContainerIsCreated
            extends GenericContainer<CustomContainerWithContainerIsCreated> {

            CustomContainerWithContainerIsCreated() {
                super(TestImages.TINY_IMAGE);
            }

            @Override
            protected void containerIsCreated(String containerId) {
                super.containerIsCreated(containerId);
            }
        }
    }

    @Nested
    class HooksTest extends AbstractReusabilityTest {

        List<String> script = new ArrayList<>();

        GenericContainer<?> container = makeReusable(
            new GenericContainer(TestImages.TINY_IMAGE) {
                @Override
                protected boolean canBeReused() {
                    // Because we override "containerIsCreated"
                    return true;
                }

                @Override
                protected void containerIsCreated(String containerId) {
                    script.add("containerIsCreated");
                }

                @Override
                protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
                    script.add("containerIsStarting(reused=" + reused + ")");
                }

                @Override
                protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
                    script.add("containerIsStarted(reused=" + reused + ")");
                }
            }
        );

        @Test
        void shouldSetLabelsIfEnvironmentDoesNotSupportReuse() {
            Mockito.doReturn(false).when(TestcontainersConfiguration.getInstance()).environmentSupportsReuse();

            String containerId = randomContainerId();
            when(client.createContainerCmd(any())).then(createContainerAnswer(containerId));
            when(client.listContainersCmd()).then(listContainersAnswer());
            when(client.startContainerCmd(containerId)).then(startContainerAnswer());
            when(client.inspectContainerCmd(containerId)).then(inspectContainerAnswer());

            container.start();
            assertThat(script)
                .containsExactly(
                    "containerIsCreated",
                    "containerIsStarting(reused=false)",
                    "containerIsStarted(reused=false)"
                );
        }

        @Test
        void shouldCallHookIfReused() {
            Mockito.doReturn(true).when(TestcontainersConfiguration.getInstance()).environmentSupportsReuse();
            String containerId = randomContainerId();
            when(client.createContainerCmd(any())).then(createContainerAnswer(containerId));
            String existingContainerId = randomContainerId();
            when(client.listContainersCmd()).then(listContainersAnswer(existingContainerId));
            when(client.inspectContainerCmd(existingContainerId)).then(inspectContainerAnswer());

            container.start();
            assertThat(script).containsExactly("containerIsStarting(reused=true)", "containerIsStarted(reused=true)");
        }

        @Test
        void shouldNotCallHookIfNotReused() {
            String containerId = randomContainerId();
            when(client.createContainerCmd(any())).then(createContainerAnswer(containerId));
            when(client.listContainersCmd()).then(listContainersAnswer());
            when(client.startContainerCmd(containerId)).then(startContainerAnswer());
            when(client.inspectContainerCmd(containerId)).then(inspectContainerAnswer());

            container.start();
            assertThat(script)
                .containsExactly(
                    "containerIsCreated",
                    "containerIsStarting(reused=false)",
                    "containerIsStarted(reused=false)"
                );
        }
    }

    @Nested
    class HashTest extends AbstractReusabilityTest {

        protected GenericContainer<?> container = makeReusable(
            new GenericContainer(TestImages.TINY_IMAGE) {
                @Override
                public void copyFileToContainer(MountableFile mountableFile, String containerPath) {
                    // NOOP
                }
            }
        );

        @Test
        void shouldStartIfListReturnsEmpty() {
            String containerId = randomContainerId();
            when(client.createContainerCmd(any())).then(createContainerAnswer(containerId));
            when(client.listContainersCmd()).then(listContainersAnswer());
            when(client.startContainerCmd(containerId)).then(startContainerAnswer());
            when(client.inspectContainerCmd(containerId)).then(inspectContainerAnswer());

            container.start();

            Mockito.verify(client, Mockito.atLeastOnce()).startContainerCmd(containerId);
        }

        @Test
        void shouldReuseIfListReturnsID() {
            Mockito.doReturn(true).when(TestcontainersConfiguration.getInstance()).environmentSupportsReuse();
            String containerId = randomContainerId();
            when(client.createContainerCmd(any())).then(createContainerAnswer(containerId));
            String existingContainerId = randomContainerId();
            when(client.listContainersCmd()).then(listContainersAnswer(existingContainerId));
            when(client.inspectContainerCmd(existingContainerId)).then(inspectContainerAnswer());

            container.start();

            Mockito.verify(client, Mockito.never()).startContainerCmd(containerId);
            Mockito.verify(client, Mockito.never()).startContainerCmd(existingContainerId);
        }

        @Test
        void shouldSetLabelsIfEnvironmentDoesNotSupportReuse() {
            Mockito.doReturn(false).when(TestcontainersConfiguration.getInstance()).environmentSupportsReuse();
            AtomicReference<CreateContainerCmd> commandRef = new AtomicReference<>();
            String containerId = randomContainerId();
            when(client.createContainerCmd(any())).then(createContainerAnswer(containerId, commandRef::set));
            when(client.startContainerCmd(containerId)).then(startContainerAnswer());
            when(client.inspectContainerCmd(containerId)).then(inspectContainerAnswer());

            container.start();

            assertThat(commandRef)
                .isNotNull()
                .satisfies(command -> {
                    assertThat(command.get().getLabels())
                        .containsKeys(DockerClientFactory.TESTCONTAINERS_SESSION_ID_LABEL);
                });
        }

        @Test
        void shouldSetCopiedFilesHashLabel() {
            Mockito.doReturn(true).when(TestcontainersConfiguration.getInstance()).environmentSupportsReuse();
            AtomicReference<CreateContainerCmd> commandRef = new AtomicReference<>();
            String containerId = randomContainerId();
            when(client.createContainerCmd(any())).then(createContainerAnswer(containerId, commandRef::set));
            when(client.listContainersCmd()).then(listContainersAnswer());
            when(client.startContainerCmd(containerId)).then(startContainerAnswer());
            when(client.inspectContainerCmd(containerId)).then(inspectContainerAnswer());

            container.start();

            assertThat(commandRef).isNotNull();
            assertThat(commandRef.get().getLabels()).containsKeys(GenericContainer.COPIED_FILES_HASH_LABEL);
        }

        @Test
        void shouldHashCopiedFiles() {
            Mockito.doReturn(true).when(TestcontainersConfiguration.getInstance()).environmentSupportsReuse();
            AtomicReference<CreateContainerCmd> commandRef = new AtomicReference<>();
            String containerId = randomContainerId();
            when(client.createContainerCmd(any())).then(createContainerAnswer(containerId, commandRef::set));
            when(client.listContainersCmd()).then(listContainersAnswer());
            when(client.startContainerCmd(containerId)).then(startContainerAnswer());
            when(client.inspectContainerCmd(containerId)).then(inspectContainerAnswer());

            container.start();

            assertThat(commandRef).isNotNull();

            Map<String, String> labels = commandRef.get().getLabels();
            assertThat(labels).containsKeys(GenericContainer.COPIED_FILES_HASH_LABEL);

            String oldHash = labels.get(GenericContainer.COPIED_FILES_HASH_LABEL);

            // Simulate stop
            container.containerId = null;

            container.withCopyFileToContainer(
                MountableFile.forClasspathResource("test_copy_to_container.txt"),
                "/foo/bar"
            );
            container.start();

            assertThat(commandRef.get().getLabels())
                .hasEntrySatisfying(
                    GenericContainer.COPIED_FILES_HASH_LABEL,
                    newHash -> {
                        assertThat(newHash).as("new hash").isNotEqualTo(oldHash);
                    }
                );
        }
    }

    @Nested
    @ParameterizedClass
    @MethodSource("strategies")
    class CopyFilesHashTest {

        private final TestStrategy strategy;

        interface TestStrategy {
            void withCopyFileToContainer(MountableFile mountableFile, String path);

            void clear();
        }

        private static class MountableFileTestStrategy implements TestStrategy {

            private final GenericContainer<?> container;

            private MountableFileTestStrategy(GenericContainer<?> container) {
                this.container = container;
            }

            @Override
            public void withCopyFileToContainer(MountableFile mountableFile, String path) {
                container.withCopyFileToContainer(mountableFile, path);
            }

            @Override
            public void clear() {
                container.getCopyToFileContainerPathMap().clear();
            }
        }

        private static class TransferableTestStrategy implements TestStrategy {

            private final GenericContainer<?> container;

            private TransferableTestStrategy(GenericContainer<?> container) {
                this.container = container;
            }

            @Override
            public void withCopyFileToContainer(MountableFile mountableFile, String path) {
                container.withCopyToContainer(mountableFile, path);
            }

            @Override
            public void clear() {
                container.getCopyToTransferableContainerPathMap().clear();
            }
        }

        public static List<Function<GenericContainer<?>, TestStrategy>> strategies() {
            return Arrays.asList(MountableFileTestStrategy::new, TransferableTestStrategy::new);
        }

        GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE);

        public CopyFilesHashTest(Function<GenericContainer<?>, TestStrategy> strategyFactory) {
            this.strategy = strategyFactory.apply(container);
        }

        @Test
        void empty() {
            assertThat(container.hashCopiedFiles()).isNotNull();
        }

        @Test
        void oneFile() {
            long emptyHash = container.hashCopiedFiles().getValue();

            strategy.withCopyFileToContainer(
                MountableFile.forClasspathResource("test_copy_to_container.txt"),
                "/foo/bar"
            );

            assertThat(container.hashCopiedFiles().getValue()).isNotEqualTo(emptyHash);
        }

        @Test
        void differentPath() {
            MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            strategy.withCopyFileToContainer(mountableFile, "/foo/bar");

            long hash1 = container.hashCopiedFiles().getValue();

            strategy.clear();

            strategy.withCopyFileToContainer(mountableFile, "/foo/baz");

            assertThat(container.hashCopiedFiles().getValue()).isNotEqualTo(hash1);
        }

        @Test
        void detectsChangesInFile() throws Exception {
            Path path = File.createTempFile("reusable_test", ".txt").toPath();
            MountableFile mountableFile = MountableFile.forHostPath(path);
            strategy.withCopyFileToContainer(mountableFile, "/foo/bar");

            long hash1 = container.hashCopiedFiles().getValue();

            Files.write(path, UUID.randomUUID().toString().getBytes());

            assertThat(container.hashCopiedFiles().getValue()).isNotEqualTo(hash1);
        }

        @Test
        void multipleFiles() {
            strategy.withCopyFileToContainer(
                MountableFile.forClasspathResource("test_copy_to_container.txt"),
                "/foo/bar"
            );
            long hash1 = container.hashCopiedFiles().getValue();

            strategy.withCopyFileToContainer(
                MountableFile.forClasspathResource("mappable-resource/test-resource.txt"),
                "/foo/baz"
            );

            assertThat(container.hashCopiedFiles().getValue()).isNotEqualTo(hash1);
        }

        @Test
        void folder() throws Exception {
            long emptyHash = container.hashCopiedFiles().getValue();

            Path tempDirectory = Files.createTempDirectory("reusable_test");
            MountableFile mountableFile = MountableFile.forHostPath(tempDirectory);
            strategy.withCopyFileToContainer(mountableFile, "/foo/bar/");

            assertThat(container.hashCopiedFiles().getValue()).isNotEqualTo(emptyHash);
        }

        @Test
        void changesInFolder() throws Exception {
            Path tempDirectory = Files.createTempDirectory("reusable_test");
            MountableFile mountableFile = MountableFile.forHostPath(tempDirectory);
            assertThat(new File(mountableFile.getResolvedPath())).isDirectory();
            strategy.withCopyFileToContainer(mountableFile, "/foo/bar/");

            long hash1 = container.hashCopiedFiles().getValue();

            Path fileInFolder = Files.createFile(
                // Create file in the sub-folder
                Files.createDirectory(tempDirectory.resolve("sub")).resolve("test.txt")
            );
            assertThat(fileInFolder).exists();
            Files.write(fileInFolder, UUID.randomUUID().toString().getBytes());

            assertThat(container.hashCopiedFiles().getValue()).isNotEqualTo(hash1);
        }

        @Test
        void folderAndFile() throws Exception {
            Path tempDirectory = Files.createTempDirectory("reusable_test");
            MountableFile mountableFile = MountableFile.forHostPath(tempDirectory);
            assertThat(new File(mountableFile.getResolvedPath())).isDirectory();
            strategy.withCopyFileToContainer(mountableFile, "/foo/bar/");

            long hash1 = container.hashCopiedFiles().getValue();

            strategy.withCopyFileToContainer(
                MountableFile.forClasspathResource("test_copy_to_container.txt"),
                "/foo/baz"
            );

            assertThat(container.hashCopiedFiles().getValue()).isNotEqualTo(hash1);
        }

        @Test
        void filePermissions() throws Exception {
            Path path = File.createTempFile("reusable_test", ".txt").toPath();
            path.toFile().setExecutable(false);
            MountableFile mountableFile = MountableFile.forHostPath(path);
            strategy.withCopyFileToContainer(mountableFile, "/foo/bar");

            long hash1 = container.hashCopiedFiles().getValue();

            assumeThat(path.toFile().canExecute()).isFalse();
            path.toFile().setExecutable(true);

            assertThat(container.hashCopiedFiles().getValue()).isNotEqualTo(hash1);
        }

        @Test
        void folderPermissions() throws Exception {
            Path tempDirectory = Files.createTempDirectory("reusable_test");
            MountableFile mountableFile = MountableFile.forHostPath(tempDirectory);
            assertThat(new File(mountableFile.getResolvedPath())).isDirectory();
            Path subDir = Files.createDirectory(tempDirectory.resolve("sub"));
            subDir.toFile().setWritable(false);
            assumeThat(subDir.toFile().canWrite()).isFalse();
            strategy.withCopyFileToContainer(mountableFile, "/foo/bar/");

            long hash1 = container.hashCopiedFiles().getValue();

            subDir.toFile().setWritable(true);
            assumeThat(subDir.toFile()).canWrite();

            assertThat(container.hashCopiedFiles().getValue()).isNotEqualTo(hash1);
        }
    }

    @ExtendWith(MockTestcontainersConfigurationExtension.class)
    public abstract static class AbstractReusabilityTest {

        protected DockerClient client = Mockito.mock(DockerClient.class);

        protected <T extends GenericContainer<?>> T makeReusable(T container) {
            container.dockerClient = client;
            container.withNetworkMode("none"); // to disable the port forwarding
            container.withStartupCheckStrategy(
                new StartupCheckStrategy() {
                    @Override
                    public boolean waitUntilStartupSuccessful(DockerClient dockerClient, String containerId) {
                        // Skip DockerClient rate limiter
                        return true;
                    }

                    @Override
                    public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
                        return StartupStatus.SUCCESSFUL;
                    }
                }
            );
            container.waitingFor(
                new AbstractWaitStrategy() {
                    @Override
                    protected void waitUntilReady() {}
                }
            );
            container.withReuse(true);
            return container;
        }

        protected String randomContainerId() {
            return UUID.randomUUID().toString();
        }

        protected Answer<ListContainersCmd> listContainersAnswer(String... ids) {
            return invocation -> {
                ListContainersCmd.Exec exec = command -> {
                    return new ObjectMapper()
                        .convertValue(
                            Stream.of(ids).map(id -> Collections.singletonMap("Id", id)).collect(Collectors.toList()),
                            new TypeReference<List<Container>>() {}
                        );
                };
                return new ListContainersCmdImpl(exec);
            };
        }

        protected Answer<CreateContainerCmd> createContainerAnswer(String containerId) {
            return createContainerAnswer(containerId, command -> {});
        }

        protected Answer<CreateContainerCmd> createContainerAnswer(
            String containerId,
            Consumer<CreateContainerCmd> cmdConsumer
        ) {
            return invocation -> {
                CreateContainerCmd.Exec exec = command -> {
                    cmdConsumer.accept(command);
                    CreateContainerResponse response = new CreateContainerResponse();
                    response.setId(containerId);
                    return response;
                };
                return new CreateContainerCmdImpl(exec, null, "image:latest");
            };
        }

        protected Answer<StartContainerCmd> startContainerAnswer() {
            return invocation -> {
                StartContainerCmd.Exec exec = command -> {
                    return null;
                };
                return new StartContainerCmdImpl(exec, invocation.getArgument(0));
            };
        }

        protected Answer<InspectContainerCmd> inspectContainerAnswer() {
            return invocation -> {
                InspectContainerCmd.Exec exec = command -> {
                    InspectContainerResponse stubResponse = Mockito.mock(
                        InspectContainerResponse.class,
                        Answers.RETURNS_DEEP_STUBS
                    );
                    when(stubResponse.getNetworkSettings().getPorts().getBindings()).thenReturn(Collections.emptyMap());
                    return stubResponse;
                };
                return new InspectContainerCmdImpl(exec, invocation.getArgument(0));
            };
        }
    }
}
