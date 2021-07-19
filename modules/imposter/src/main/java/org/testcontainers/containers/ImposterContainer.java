package org.testcontainers.containers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Cleanup;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static java.util.Objects.nonNull;

/**
 * Mocks from OpenAPI/Swagger specifications using Imposter.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ImposterContainer<SELF extends ImposterContainer<SELF>> extends GenericContainer<SELF> implements LinkableContainer {
    public static final int IMPOSTER_DEFAULT_PORT = 8080;
    public static final String COMBINED_SPECIFICATION_URL = "/_spec/combined.json";
    public static final String SPECIFICATION_UI_URL = "/_spec/";
    private static final String CONTAINER_CONFIG_DIR = "/opt/imposter/config";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private Path configurationDir;
    private List<Path> specificationFiles = new ArrayList<>();

    public ImposterContainer() {
        this("outofcoffee/imposter-openapi:1.2.0");
    }

    public ImposterContainer(String dockerImageName) {
        super(dockerImageName);

        addExposedPort(IMPOSTER_DEFAULT_PORT);
        setCommand(
            "--configDir", CONTAINER_CONFIG_DIR
        );

        // wait for the engine to parse and combine the specifications
        waitingFor(Wait.forHttp(ImposterContainer.COMBINED_SPECIFICATION_URL));
    }

    @Override
    protected void doStart() {
        try {
            if (!specificationFiles.isEmpty() && nonNull(configurationDir)) {
                throw new IllegalStateException("Must specify only one of specification file or specification directory");
            }

            if (!specificationFiles.isEmpty()) {
                final Path configDir = writeImposterConfig(specificationFiles);
                this.setConfigurationDir(configDir);
            }

            if (nonNull(configurationDir)) {
                addFileSystemBind(configurationDir.toString(), CONTAINER_CONFIG_DIR, BindMode.READ_ONLY);
            } else {
                throw new IllegalStateException("Must specify one of specification file or specification directory");
            }

        } catch (Exception e) {
            throw new ContainerLaunchException("Error starting Imposter container", e);
        }

        super.doStart();

        logger().info("Started Imposter mock engine" +
            "\n  Specification UI: " + getSpecificationUiUri() +
            "\n  Config dir: " + configurationDir);
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Collections.singleton(getMappedPort(IMPOSTER_DEFAULT_PORT));
    }

    public URL getBaseUrl(String scheme, int port) throws MalformedURLException {
        return new URL(scheme + "://" + getContainerIpAddress() + ":" + getMappedPort(port));
    }

    public URL getBaseUrl(String scheme) throws MalformedURLException {
        return getBaseUrl(scheme, ImposterContainer.IMPOSTER_DEFAULT_PORT);
    }

    public URL getBaseUrl() throws MalformedURLException {
        return getBaseUrl("http");
    }

    /**
     * The directory containing a valid Imposter configuration file.
     *
     * @param configurationDir the directory
     */
    public void setConfigurationDir(Path configurationDir) {
        this.configurationDir = configurationDir;
    }

    /**
     * The directory containing a valid Imposter configuration file.
     *
     * @param configurationDir the directory
     */
    public SELF withConfigurationDir(String configurationDir) {
        return withConfigurationDir(Paths.get(configurationDir));
    }

    /**
     * The directory containing a valid Imposter configuration file.
     *
     * @param configurationDir the directory
     */
    public SELF withConfigurationDir(Path configurationDir) {
        this.setConfigurationDir(configurationDir);
        return self();
    }

    /**
     * The path to a valid OpenAPI/Swagger specification file.
     *
     * @param specificationFile the directory
     */
    public SELF withSpecificationFile(String specificationFile) {
        return withSpecificationFile(Paths.get(specificationFile));
    }

    /**
     * The path to a valid OpenAPI/Swagger specification file.
     *
     * @param specificationFile the directory
     */
    public SELF withSpecificationFile(Path specificationFile) {
        this.addSpecificationFile(specificationFile);
        return self();
    }

    /**
     * The path to a valid OpenAPI/Swagger specification file.
     *
     * @param specificationFile the directory
     */
    public void addSpecificationFile(Path specificationFile) {
        this.specificationFiles.add(specificationFile);
    }

    private Path writeImposterConfig(List<Path> specificationFiles) throws IOException {
        final Path configDir = Files.createTempDirectory("imposter");
        specificationFiles.forEach(spec -> {
            try {
                // copy spec into place
                Files.copy(spec, configDir.resolve(spec.getFileName()));

                // write config file
                final Path configFile = configDir.resolve(spec.getFileName() + "-config.json");
                final @Cleanup FileOutputStream out = new FileOutputStream(configFile.toFile());

                MAPPER.writeValue(out, new HashMap<String, Object>() {{
                    put("plugin", "openapi");
                    put("specFile", spec.getFileName().toString());
                }});

                logger().debug("Wrote Imposter configuration file: {}", configFile);

            } catch (IOException e) {
                throw new RuntimeException(String.format("Error generating configuration for specification file %s in %s", spec, configDir), e);
            }
        });
        return configDir;
    }

    public URI getCombinedSpecificationUri() {
        try {
            return URI.create(getBaseUrl("http", IMPOSTER_DEFAULT_PORT) + COMBINED_SPECIFICATION_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error getting combined specification URI", e);
        }
    }

    public URI getSpecificationUiUri() {
        try {
            return URI.create(getBaseUrl("http", IMPOSTER_DEFAULT_PORT) + SPECIFICATION_UI_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error getting specification UI URI", e);
        }
    }
}
