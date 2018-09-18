package org.testcontainers;

import com.github.dockerjava.api.DockerClient;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.ResourceCleaner;
import org.testcontainers.utility.TestcontainersConfiguration;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Cleans allocated docker resources via side JVM process.
 */
class SideProcessResourceManager extends ResourceManagerBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SideProcessResourceManager.class);
    private static final String FILE = "file";
    private static final String JAR = "jar";
    private static final String JAR_DELIMITER = "!";
    private static final String PROTOCOL_DELIMITER = ":";

    private static final List<List<Map.Entry<String, String>>> DEATH_NOTE = new ArrayList<>();

    SideProcessResourceManager(DockerClient dockerClient) {
        super(dockerClient);
    }

    @Override
    @SneakyThrows(Exception.class)
    public void initialize() {
        ServerSocket serverSocket = new ServerSocket(0);
        CountDownLatch sideProcessLatch = new CountDownLatch(1);

        synchronized (DEATH_NOTE) {
            DEATH_NOTE.add(
                DockerClientFactory.DEFAULT_LABELS.entrySet().stream()
                    .<Map.Entry<String, String>>map(it -> new AbstractMap.SimpleEntry<>("label", it.getKey() + "=" + it.getValue()))
                    .collect(Collectors.toList())
            );
        }

        Thread kiraThread = new Thread(
            DockerClientFactory.TESTCONTAINERS_THREAD_GROUP,
            () -> {
                try (Socket socket = serverSocket.accept()) {
                    FilterRegistry registry = new FilterRegistry(socket.getInputStream(), socket.getOutputStream());
                    int index = 0;
                    synchronized (DEATH_NOTE) {
                        while (true) {
                            if (DEATH_NOTE.size() <= index) {
                                try {
                                    DEATH_NOTE.wait(1_000);
                                    continue;
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            List<Map.Entry<String, String>> filters = DEATH_NOTE.get(index);
                            boolean isAcknowledged = registry.register(filters);
                            if (isAcknowledged) {
                                LOGGER.debug("Received 'ACK' from side process");
                                sideProcessLatch.countDown();
                                index++;
                            } else {
                                LOGGER.debug("Didn't receive 'ACK' from side process. Will retry to send filters.");
                            }
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warn("Can not communicate with side process: {}", e.getMessage());
                }
            },
            "testcontainers-side-process"
        );
        kiraThread.setDaemon(true);
        kiraThread.start();

        if (!startProcess(serverSocket.getLocalPort())) {
            throw new IllegalStateException("Can not start side process");
        }

        // We need to wait before we can start any containers to make sure that we delete them
        if (!sideProcessLatch.await(TestcontainersConfiguration.getInstance().getRyukTimeout(), TimeUnit.SECONDS)) {
            throw new IllegalStateException("Can not connect to side process");
        }

        LOGGER.info("Side process cleaner started - will monitor and terminate Testcontainers containers on JVM exit");
    }

    private boolean startProcess(int portNumber) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();

        // Write command line in temp file to start side process
        Path tempFile = Files.createTempFile(
            "testcontainers-resource-cleaner-",
            DockerClientFactory.SESSION_ID + ".bat");
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
            writer.write(
                escapeArgument(getJavaPath()) +
                " -cp " +
                escapeArgument(getClassPath()) + " " +
                ResourceCleaner.class.getName() + " " +
                portNumber + "\n" +
                "start \"\" cmd /c del \"%~f0\"&exit" // remove temp file on exit
            );
        }

        processBuilder.command(Arrays.asList(
            "cmd", "/c", "start", "/min",
            String.format("\"Testcontainers session %s\"", DockerClientFactory.SESSION_ID),
            tempFile.toString(), "&", "exit"
        ));

        try {
            LOGGER.debug("Starting side process: {}", String.join(" ", processBuilder.command()));
            processBuilder.start();
            return true;
        } catch (IOException e) {
            LOGGER.warn("Can not start side process: {}", e.getMessage());
        }

        return false;
    }

    @Override
    public void registerFilterForCleanup(List<Map.Entry<String, String>> filter) {
        synchronized (DEATH_NOTE) {
            DEATH_NOTE.add(filter);
            DEATH_NOTE.notifyAll();
        }
    }

    private static String getJavaPath() {
        return Paths.get(System.getProperty("java.home"), "bin", "java").toString();
    }

    private String getClassPath() {
        Set<String> classPaths = Stream.of(
            ResourceCleaner.class,
            com.github.dockerjava.api.DockerClient.class,
            org.apache.http.client.utils.URLEncodedUtils.class
        )
            .map(SideProcessResourceManager::getClassResource)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        classPaths.addAll(Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator)));

        return String.join(File.pathSeparator, classPaths);
    }

    private static String escapeArgument(@NotNull String argument) {
        return argument.contains(" ") ? String.format("\"%s\"", argument) : argument;
    }

    @Nullable
    public static String getClassResource(@NotNull Class<?> klass) {
        String resourcePath = klass.getName().replace('.', '/') + ".class";
        URL resource = klass.getClassLoader().getResource(resourcePath);
        return resource != null ? extractRoot(resource, resourcePath) : null;
    }

    /**
     * Attempts to extract classpath entry part from passed URL.
     */
    private static String extractRoot(URL resourceURL, String resourcePath) {
        String protocol = resourceURL.getProtocol();
        String resultPath = null;

        if (FILE.equals(protocol)) {
            String path = resourceURL.getFile();
            String testPath = path.replace('\\', '/').toLowerCase();
            String testResourcePath = resourcePath.replace('\\', '/').toLowerCase();
            if (testPath.endsWith(testResourcePath)) {
                resultPath = path.substring(0, path.length() - resourcePath.length());
            }
        }
        else if (JAR.equals(protocol)) {
            String fullPath = resourceURL.getFile();
            int delimiter = fullPath.indexOf(JAR_DELIMITER);
            if (delimiter >= 0) {
                String archivePath = fullPath.substring(0, delimiter);
                if (archivePath.startsWith(FILE + PROTOCOL_DELIMITER)) {
                    resultPath = archivePath.substring(FILE.length() + PROTOCOL_DELIMITER.length());
                }
            }
        }

        if (resultPath == null) {
            return null;
        }

        if (resultPath.endsWith(File.separator)) {
            resultPath = resultPath.substring(0, resultPath.length() - 1);
        }

        resultPath = resultPath.replaceAll("%20", " ");
        resultPath = resultPath.replaceAll("%23", "#");

        // !Workaround for /D:/some/path/a.jar, which doesn't work if D is subst disk
        if (resultPath.startsWith("/") && resultPath.indexOf(":") == 2) {
            resultPath = resultPath.substring(1);
        }

        return resultPath;
    }
}
