/*
 * MIT License
 *
 * Copyright (c) 2021-present HiveMQ GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.testcontainers.hivemq;

import com.hivemq.extension.sdk.api.ExtensionMain;
import javassist.ClassPool;
import javassist.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Yannick Weber
 */
@SuppressWarnings("UnusedReturnValue")
public class HiveMQContainer extends GenericContainer<HiveMQContainer> {

    private final static @NotNull Logger logger = LoggerFactory.getLogger(HiveMQContainer.class);

    private static final @NotNull String validPluginXML =
            "<hivemq-extension>" + //
                    "   <id>%s</id>" + //
                    "   <name>%s</name>" + //
                    "   <version>%s</version>" + //
                    "   <priority>%s</priority>" +  //
                    "   <start-priority>%s</start-priority>" +  //
                    "</hivemq-extension>";

    private static final @NotNull DockerImageName DEFAULT_HIVEMQ_CE_IMAGE_NAME = DockerImageName.parse("hivemq/hivemq-ce");
    private static final @NotNull DockerImageName DEFAULT_HIVEMQ_EE_IMAGE_NAME = DockerImageName.parse("hivemq/hivemq4");
    private static final @NotNull String DEFAULT_HIVEMQ_CE_TAG = "2021.3";

    public static final int DEBUGGING_PORT = 9000;
    public static final int MQTT_PORT = 1883;
    public static final int CONTROL_CENTER_PORT = 8080;
    @SuppressWarnings("OctalInteger")
    private static final int MODE = 0777;
    private static final @NotNull Pattern EXTENSION_ID_PATTERN = Pattern.compile("<id>(.+?)</id>");

    private final @NotNull ConcurrentHashMap<String, CountDownLatch> containerOutputLatches = new ConcurrentHashMap<>();
    private volatile boolean silent = false;
    private volatile boolean controlCenterEnabled = false;

    private final @NotNull MultiLogMessageWaitStrategy waitStrategy = new MultiLogMessageWaitStrategy();

    public HiveMQContainer() {
        this(DEFAULT_HIVEMQ_CE_IMAGE_NAME.withTag(DEFAULT_HIVEMQ_CE_TAG));
    }

    public HiveMQContainer(final @NotNull DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_HIVEMQ_CE_IMAGE_NAME, DEFAULT_HIVEMQ_EE_IMAGE_NAME);

        addExposedPort(MQTT_PORT);

        waitStrategy.withRegEx("(.*)Started HiveMQ in(.*)");
        waitingFor(waitStrategy);

        withLogConsumer(outputFrame -> {
            final String utf8String = outputFrame.getUtf8String();
            if (utf8String.startsWith("Listening for transport dt_socket at address:")) {
                System.out.println("Listening for transport dt_socket at address: " + getMappedPort(DEBUGGING_PORT));
            } else if (!silent) {
                System.out.print(utf8String);
            }
        });
        withLogConsumer((outputFrame) -> {
            if (!containerOutputLatches.isEmpty()) {
                containerOutputLatches.forEach((regEx, latch) -> {
                    if (outputFrame.getUtf8String().matches("(?s)" + regEx)) {
                        logger.debug("Container Output '{}' matched RegEx '{}'", outputFrame.getUtf8String(), regEx);
                        latch.countDown();
                    } else {
                        logger.debug("Container Output '{}' did not match RegEx '{}'", outputFrame.getUtf8String(), regEx);
                    }
                });
            }
        });
    }

    @Override
    public void start() {
        super.start();
        if (controlCenterEnabled) {
            logger.info("The HiveMQ Control Center is reachable under: http://localhost:{}", getMappedPort(CONTROL_CENTER_PORT));
        }
    }

    /**
     * Adds a wait condition for the extension with this name.
     * <p>
     * Must be called before the container is started.
     *
     * @param extensionName the extension to wait for
     * @return self
     */
    public @NotNull HiveMQContainer waitForExtension(final @NotNull String extensionName) {
        final String regEX = "(.*)Extension \"" + extensionName + "\" version (.*) started successfully(.*)";
        waitStrategy.withRegEx(regEX);
        return self();
    }

    /**
     * Adds a wait condition for this {@link HiveMQExtension}
     * <p>
     * Must be called before the container is started.
     *
     * @param extension the extension to wait for
     * @return self
     */
    public @NotNull HiveMQContainer waitForExtension(final @NotNull HiveMQExtension extension) {
        return this.waitForExtension(extension.getName());
    }

    /**
     * Enables the possibility for remote debugging clients to connect.
     * <p>
     * Must be called before the container is started.
     *
     * @return self
     */
    public @NotNull HiveMQContainer withDebugging() {
        addExposedPorts(DEBUGGING_PORT);
        withEnv("JAVA_OPTS", "-agentlib:jdwp=transport=dt_socket,address=0.0.0.0:" + DEBUGGING_PORT + ",server=y,suspend=y");
        return self();
    }

    /**
     * Sets the logging {@link Level} inside the container.
     * <p>
     * Must be called before the container is started.
     *
     * @param level the {@link Level}
     * @return self
     */
    public @NotNull HiveMQContainer withLogLevel(final @NotNull Level level) {
        this.withEnv("HIVEMQ_LOG_LEVEL", level.name());
        return self();
    }

    /**
     * Wraps the given class and all its subclasses into an extension
     * and puts it into '/opt/hivemq/extensions/{extension-id}' inside the container.
     * <p>
     * Must be called before the container is started.
     *
     * @param hiveMQExtension the {@link HiveMQExtension} of the extension
     * @return self
     */
    public @NotNull HiveMQContainer withExtension(final @NotNull HiveMQExtension hiveMQExtension) {
        try {
            final File extension = createExtension(hiveMQExtension);
            final MountableFile mountableExtension = MountableFile.forHostPath(extension.getPath(), MODE);
            withCopyFileToContainer(mountableExtension, "/opt/hivemq/extensions/" + hiveMQExtension.getId());
        } catch (final Exception e) {
            throw new ContainerLaunchException(e.getMessage() == null ? "" : e.getMessage(), e);
        }
        return self();
    }

    /**
     * Puts the given extension folder into '/opt/hivemq/extensions/{directory-name}' inside the container.
     * It must at least contain a valid hivemq-extension.xml and a valid extension.jar in order to be executed.
     * The directory-name is taken from the id defined in the hivemq-extension.xml.
     * <p>
     * Must be called before the container is started.
     *
     * @param mountableExtension the extension folder on the host machine
     * @return self
     */
    public @NotNull HiveMQContainer withExtension(final @NotNull MountableFile mountableExtension) {
        final File extensionDir = new File(mountableExtension.getResolvedPath());
        if (!extensionDir.exists()) {
            throw new ContainerLaunchException("Extension '" + mountableExtension.getFilesystemPath() + "' could not be mounted. It does not exist.");
        }
        if (!extensionDir.isDirectory()) {
            throw new ContainerLaunchException("Extension '" + mountableExtension.getFilesystemPath() + "' could not be mounted. It is not a directory.");
        }
        try {
            final String extensionDirName = getExtensionDirectoryName(extensionDir);
            final String containerPath = "/opt/hivemq/extensions/" + extensionDirName;
            withCopyFileToContainer(cloneWithFileMode(mountableExtension, MODE), containerPath);
            logger.info("Putting extension '{}' into '{}'", extensionDirName, containerPath);
        } catch (final Exception e) {
            throw new ContainerLaunchException(e.getMessage() == null ? "" : e.getMessage(), e);
        }
        return self();
    }

    private @NotNull String getExtensionDirectoryName(final @NotNull File extensionDirectory) throws IOException {
        final File file = new File(extensionDirectory, "hivemq-extension.xml");
        final String xml = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        final Matcher matcher = EXTENSION_ID_PATTERN.matcher(xml);

        if (!matcher.find()) {
            throw new IllegalStateException("Could not parse extension id from '" + file.getAbsolutePath() + "'");
        }
        return matcher.group(1);
    }

    private @NotNull File createExtension(final @NotNull HiveMQExtension hiveMQExtension)
            throws Exception {

        final File tempDir = Files.createTempDirectory("").toFile();

        final File extensionDir = new File(tempDir, hiveMQExtension.getId());
        FileUtils.writeStringToFile(new File(extensionDir, "hivemq-extension.xml"),
                String.format(
                        validPluginXML,
                        hiveMQExtension.getId(),
                        hiveMQExtension.getName(),
                        hiveMQExtension.getVersion(),
                        hiveMQExtension.getPriority(),
                        hiveMQExtension.getStartPriority()),
                Charset.defaultCharset());

        if (hiveMQExtension.isDisabledOnStartup()) {
            final File disabled = new File(extensionDir, "DISABLED");
            final boolean newFile = disabled.createNewFile();
            if (!newFile) {
                throw new ContainerLaunchException("Could not create DISABLED file '" + disabled.getAbsolutePath() + "' on host machine.");
            }
        }

        final JavaArchive javaArchive =
                ShrinkWrap.create(JavaArchive.class)
                        .addAsServiceProviderAndClasses(ExtensionMain.class, hiveMQExtension.getMainClass());

        putSubclassesIntoJar(hiveMQExtension.getId(), hiveMQExtension.getMainClass(), javaArchive);
        for (final Class<?> additionalClass : hiveMQExtension.getAdditionalClasses()) {
            javaArchive.addClass(additionalClass);
            putSubclassesIntoJar(hiveMQExtension.getId(), additionalClass, javaArchive);
        }

        javaArchive.as(ZipExporter.class).exportTo(new File(extensionDir, "extension.jar"));

        return extensionDir;
    }

    private void putSubclassesIntoJar(
            final @NotNull String extensionId,
            final @Nullable Class<?> clazz,
            final @NotNull JavaArchive javaArchive) throws NotFoundException {

        if (clazz != null) {
            final Set<String> subClassNames =
                    ClassPool.getDefault().get(clazz.getName()).getClassFile().getConstPool().getClassNames();
            for (final String subClassName : subClassNames) {
                final String className = subClassName.replaceAll("/", ".");

                if (!className.startsWith("[L")) {
                    logger.debug("Trying to package subclass '{}' into extension '{}'.", className, extensionId);
                    javaArchive.addClass(className);
                } else {
                    logger.debug("Class '{}' will be ignored.", className);
                }
            }
        }
    }

    /**
     * Removes the specified prepackaged extension folders from '/opt/hivemq/extensions' before the container is built.
     * Note: this creates a custom docker image.
     * <p>
     * Must be called before the container is started.
     *
     * @param extensionIds the prepackaged extensions to remove
     * @return self
     */
    public @NotNull HiveMQContainer withoutPrepackagedExtensions(final @NotNull String... extensionIds) {
        final String dockerImageName = getDockerImageName();
        setImage(new ImageFromDockerfile(dockerImageName + "-custom")
                .withDockerfileFromBuilder(builder -> {
                    builder.from(dockerImageName);
                    for (final String extensionId : extensionIds) {
                        builder.run("rm", "-rf", "/opt/hivemq/extensions/" + extensionId);
                    }
                }));
        return self();
    }

    /**
     * Removes all prepackaged extension folders from '/opt/hivemq/extensions' before the container is built.
     * Note: this creates a custom docker image.
     * <p>
     * Must be called before the container is started.
     *
     * @return self
     */
    public @NotNull HiveMQContainer withoutPrepackagedExtensions() {
        final String dockerImageName = getDockerImageName();
        setImage(new ImageFromDockerfile(dockerImageName + "-custom")
                .withDockerfileFromBuilder(builder ->
                        builder.from(dockerImageName)
                                .run("rm", "-rf", "/opt/hivemq/extensions/")));
        return self();
    }

    /**
     * Puts the given license into '/opt/hivemq/license/' inside the container.
     * It must end with '.lic' or '.elic'.
     * <p>
     * Must be called before the container is started.
     *
     * @param mountableLicense the license file on the host machine
     * @return self
     */
    public @NotNull HiveMQContainer withLicense(final @NotNull MountableFile mountableLicense) {
        final File licenseFile = new File(mountableLicense.getResolvedPath());
        if (!licenseFile.exists()) {
            throw new ContainerLaunchException("License file '" + mountableLicense.getFilesystemPath() + "' does not exist.");
        }
        if (!licenseFile.getName().endsWith(".lic") && !licenseFile.getName().endsWith(".elic")) {
            throw new ContainerLaunchException("License file '" + mountableLicense.getFilesystemPath() + "' does not end wit '.lic' or '.elic'.");
        }
        final String containerPath = "/opt/hivemq/license/" + licenseFile.getName();
        withCopyFileToContainer(cloneWithFileMode(mountableLicense, MODE), containerPath);
        logger.info("Putting license '{}' into '{}'.", licenseFile.getAbsolutePath(), containerPath);
        return self();
    }

    /**
     * Overwrites the HiveMQ configuration in '/opt/hivemq/conf/' inside the container.
     * <p>
     * Must be called before the container is started.
     *
     * @param mountableConfig the config file on the host machine
     * @return self
     */
    public @NotNull HiveMQContainer withHiveMQConfig(final @NotNull MountableFile mountableConfig) {
        final File config = new File(mountableConfig.getResolvedPath());
        if (!config.exists()) {
            throw new ContainerLaunchException("HiveMQ config file '" + mountableConfig.getFilesystemPath() + "' does not exist.");
        }
        final String containerPath = "/opt/hivemq/conf/config.xml";
        withCopyFileToContainer(cloneWithFileMode(mountableConfig, MODE), containerPath);
        logger.info("Putting '{}' into '{}'.", config.getAbsolutePath(), containerPath);
        return self();
    }

    /**
     * Puts the given file into the root of the extension's home '/opt/hivemq/extensions/{extensionId}/'.
     * Note: the extension must be loaded before the file is put.
     * <p>
     * Must be called before the container is started.
     *
     * @param file        the file on the host machine
     * @param extensionId the extension
     * @return self
     */
    public @NotNull HiveMQContainer withFileInExtensionHomeFolder(
            final @NotNull MountableFile file,
            final @NotNull String extensionId) {

        return withFileInExtensionHomeFolder(file, extensionId, "");
    }

    /**
     * Puts the given file into given subdirectory of the extensions's home '/opt/hivemq/extensions/{id}/{pathInExtensionHome}/'
     * Note: the extension must be loaded before the file is put.
     * <p>
     * Must be called before the container is started.
     *
     * @param file                the file on the host machine
     * @param extensionId         the extension
     * @param pathInExtensionHome the path
     * @return self
     */
    public @NotNull HiveMQContainer withFileInExtensionHomeFolder(
            final @NotNull MountableFile file,
            final @NotNull String extensionId,
            final @NotNull String pathInExtensionHome) {

        return withFileInHomeFolder(file, "/extensions/" + extensionId + PathUtil.prepareAppendPath(pathInExtensionHome));
    }

    /**
     * Puts the given file into the given subdirectory of the HiveMQ home folder '/opt/hivemq/{pathInHomeFolder}'.
     * <p>
     * Must be called before the container is started.
     *
     * @param mountableFile    the file on the host machine
     * @param pathInHomeFolder the path
     * @return self
     */
    public @NotNull HiveMQContainer withFileInHomeFolder(
            final @NotNull MountableFile mountableFile,
            final @NotNull String pathInHomeFolder) {

        final File file = new File(mountableFile.getResolvedPath());

        if (pathInHomeFolder.trim().isEmpty()) {
            throw new ContainerLaunchException("pathInHomeFolder must not be empty");
        }

        if (!file.exists()) {
            throw new ContainerLaunchException("File '" + mountableFile.getFilesystemPath() + "‘ does not exist.");
        }
        final String containerPath = "/opt/hivemq" + PathUtil.prepareAppendPath(pathInHomeFolder);
        withCopyFileToContainer(cloneWithFileMode(mountableFile, MODE), containerPath);
        logger.info("Putting file '{}' into container path '{}'.", file.getAbsolutePath(), containerPath);
        return self();
    }

    /**
     * Disables the extension with the given name and extension directory name.
     * This method blocks until the HiveMQ log for successful disabling is consumed or it times out after {timeOut}.
     * Note: Disabling Extensions is a HiveMQ Enterprise feature, it will not work when using the HiveMQ Community Edition.
     * <p>
     * This can only be called once the container is started.
     *
     * @param extensionName      the name of the extension to disable
     * @param extensionDirectory the name of the extension's directory
     * @param timeout            the timeout
     * @throws TimeoutException if the extension was not disabled within the configured timeout
     */
    public void disableExtension(
            final @NotNull String extensionName,
            final @NotNull String extensionDirectory,
            final @NotNull Duration timeout) throws TimeoutException {

        final String regEX = "(.*)Extension \"" + extensionName + "\" version (.*) stopped successfully(.*)";
        try {
            final String containerPath = "/opt/hivemq/extensions" + PathUtil.prepareInnerPath(extensionDirectory) + "DISABLED";

            final CountDownLatch latch = new CountDownLatch(1);
            containerOutputLatches.put(regEX, latch);

            execInContainer("touch", containerPath);
            logger.info("Putting DISABLED file into container path '{}'", containerPath);

            final boolean await = latch.await(timeout.getSeconds(), TimeUnit.SECONDS);
            if (!await) {
                throw new TimeoutException("Extension disabling timed out after '" + timeout.getSeconds() + "' seconds. " +
                        "Maybe you are using a HiveMQ Community Edition image, " +
                        "which does not support disabling of extensions");
            }
        } catch (final InterruptedException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            containerOutputLatches.remove(regEX);
        }
    }

    /**
     * Disables the extension with the given name and extension directory name.
     * This method blocks until the HiveMQ log for successful disabling is consumed or it times out after 60 seconds.
     * Note: Disabling Extensions is a HiveMQ Enterprise feature, it will not work when using the HiveMQ Community Edition.
     * <p>
     * This can only be called once the container is started.
     *
     * @param extensionName      the name of the extension to disable
     * @param extensionDirectory the name of the extension's directory
     * @throws TimeoutException if the extension was not disabled within 60 seconds
     */
    public void disableExtension(
            final @NotNull String extensionName,
            final @NotNull String extensionDirectory) throws TimeoutException {
        disableExtension(extensionName, extensionDirectory, Duration.ofSeconds(60));
    }

    /**
     * Disables the extension.
     * This method blocks until the HiveMQ log for successful disabling is consumed or it times out after {timeOut}.
     * Note: Disabling Extensions is a HiveMQ Enterprise feature, it will not work when using the HiveMQ Community Edition.
     * <p>
     * This can only be called once the container is started.
     *
     * @param hiveMQExtension the extension
     * @param timeout         the timeout
     * @throws TimeoutException if the extension was not disabled within the configured timeout
     */
    public void disableExtension(
            final @NotNull HiveMQExtension hiveMQExtension,
            final @NotNull Duration timeout) throws TimeoutException {
        disableExtension(hiveMQExtension.getName(), hiveMQExtension.getId(), timeout);
    }

    /**
     * Disables the extension.
     * This method blocks until the HiveMQ log for successful disabling is consumed or it times out after 60 seconds.
     * Note: Disabling Extensions is a HiveMQ Enterprise feature, it will not work when using the HiveMQ Community Edition.
     * <p>
     * This can only be called once the container is started.
     *
     * @param hiveMQExtension the extension
     * @throws TimeoutException if the extension was not disabled within 60 seconds
     */
    public void disableExtension(final @NotNull HiveMQExtension hiveMQExtension) throws TimeoutException {
        disableExtension(hiveMQExtension, Duration.ofSeconds(60));
    }

    /**
     * Enables the extension with the given name and extension directory name.
     * This method blocks until the HiveMQ log for successful enabling is consumed or it times out after {timeOut}.
     * Note: Enabling Extensions is a HiveMQ Enterprise feature, it will not work when using the HiveMQ Community Edition.
     * <p>
     * This can only be called once the container is started.
     *
     * @param extensionName      the name of the extension to disable
     * @param extensionDirectory the name of the extension's directory
     * @param timeout            the timeout
     * @throws TimeoutException if the extension was not enabled within the configured timeout
     */
    public void enableExtension(
            final @NotNull String extensionName,
            final @NotNull String extensionDirectory,
            final @NotNull Duration timeout) throws TimeoutException {

        final String regEX = "(.*)Extension \"" + extensionName + "\" version (.*) started successfully(.*)";
        try {
            final String containerPath = "/opt/hivemq/extensions" + PathUtil.prepareInnerPath(extensionDirectory) + "DISABLED";

            final CountDownLatch latch = new CountDownLatch(1);
            containerOutputLatches.put(regEX, latch);

            execInContainer("rm", "-rf", containerPath);
            logger.info("Removing DISABLED file in container path '{}'", containerPath);

            final boolean await = latch.await(timeout.getSeconds(), TimeUnit.SECONDS);
            if (!await) {
                throw new TimeoutException("Extension enabling timed out after '" + timeout.getSeconds() + "' seconds. " +
                        "Maybe you are using a HiveMQ Community Edition image, " +
                        "which does not support disabling of extensions");
            }
        } catch (final InterruptedException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            containerOutputLatches.remove(regEX);
        }
    }

    /**
     * Enables the extension with the given name and extension directory name.
     * This method blocks until the HiveMQ log for successful enabling is consumed or it times out after 60 seconds.
     * Note: Enabling Extensions is a HiveMQ Enterprise feature, it will not work when using the HiveMQ Community Edition.
     * <p>
     * This can only be called once the container is started.
     *
     * @param extensionName      the name of the extension to disable
     * @param extensionDirectory the name of the extension's directory
     * @throws TimeoutException if the extension was not enabled within 60 seconds
     */
    public void enableExtension(
            final @NotNull String extensionName,
            final @NotNull String extensionDirectory) throws TimeoutException {
        enableExtension(extensionName, extensionDirectory, Duration.ofSeconds(60));
    }

    /**
     * Enables the extension.
     * This method blocks until the HiveMQ log for successful enabling is consumed or it times out after {timeOut}.
     * Note: Enabling Extensions is a HiveMQ Enterprise feature, it will not work when using the HiveMQ Community Edition.
     * <p>
     * This can only be called once the container is started.
     *
     * @param hiveMQExtension the extension
     * @param timeout         the timeout
     * @throws TimeoutException if the extension was not enabled within the configured timeout
     */
    public void enableExtension(
            final @NotNull HiveMQExtension hiveMQExtension,
            final @NotNull Duration timeout) throws TimeoutException {
        enableExtension(hiveMQExtension.getName(), hiveMQExtension.getId(), timeout);
    }

    /**
     * Enables the extension.
     * This method blocks until the HiveMQ log for successful enabling is consumed or it times out after {timeOut}.
     * Note: Enabling Extensions is a HiveMQ Enterprise feature, it will not work when using the HiveMQ Community Edition.
     * <p>
     * This can only be called once the container is started.
     *
     * @param hiveMQExtension the extension
     * @throws TimeoutException if the extension was not enabled within 60 seconds
     */
    public void enableExtension(final @NotNull HiveMQExtension hiveMQExtension) throws TimeoutException {
        enableExtension(hiveMQExtension, Duration.ofSeconds(60));
    }

    /**
     * Determines whether the stdout of the container is printed to System.out.
     *
     * @param silent whether the container is silent.
     * @return self
     */
    public @NotNull HiveMQContainer silent(final boolean silent) {
        this.silent = silent;
        return self();
    }

    /**
     * Enables connection to the HiveMQ Control Center on host port 8080.
     * Note: the control center is a HiveMQ 4 Enterprise feature.
     * <p>
     * Must be called before the container is started.
     *
     * @return self
     */
    public @NotNull HiveMQContainer withControlCenter() {
        addExposedPorts(CONTROL_CENTER_PORT);
        controlCenterEnabled = true;
        return self();
    }

    /**
     * Get the mapped port for the MQTT port of the container.
     * <p>
     * Must be called after the container is started.
     *
     * @return the port on the host machine for mqtt clients to connect
     */
    public int getMqttPort() {
        return this.getMappedPort(MQTT_PORT);
    }

    @Override
    public void stop() {
        waitStrategy.reset();
        super.stop();
    }

    private @NotNull MountableFile cloneWithFileMode(final @NotNull MountableFile mountableFile, final int mode) {
        return MountableFile.forHostPath(mountableFile.getResolvedPath(), mode);
    }
}
