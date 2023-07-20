package org.testcontainers.redpanda;

import com.github.dockerjava.api.command.InspectContainerResponse;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Testcontainers implementation for Redpanda.
 */
public class RedpandaContainer extends GenericContainer<RedpandaContainer> {

    private static final String REDPANDA_FULL_IMAGE_NAME = "docker.redpanda.com/redpandadata/redpanda";

    @Deprecated
    private static final String REDPANDA_OLD_FULL_IMAGE_NAME = "docker.redpanda.com/vectorized/redpanda";

    private static final DockerImageName REDPANDA_IMAGE = DockerImageName.parse(REDPANDA_FULL_IMAGE_NAME);

    @Deprecated
    private static final DockerImageName REDPANDA_OLD_IMAGE = DockerImageName.parse(REDPANDA_OLD_FULL_IMAGE_NAME);

    private static final int REDPANDA_PORT = 9092;

    private static final int REDPANDA_ADMIN_PORT = 9644;

    private static final int SCHEMA_REGISTRY_PORT = 8081;

    private boolean enableAuthorization;

    private String authenticationMethod = "none";

    private String schemaRegistryAuthenticationMethod = "none";

    private final List<String> superusers = new ArrayList<>();

    public RedpandaContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public RedpandaContainer(DockerImageName imageName) {
        super(imageName);
        imageName.assertCompatibleWith(REDPANDA_OLD_IMAGE, REDPANDA_IMAGE);

        boolean isLessThanBaseVersion = new ComparableVersion(imageName.getVersionPart()).isLessThan("v22.2.1");
        if (REDPANDA_FULL_IMAGE_NAME.equals(imageName.getUnversionedPart()) && isLessThanBaseVersion) {
            throw new IllegalArgumentException("Redpanda version must be >= v22.2.1");
        }

        withExposedPorts(REDPANDA_PORT, REDPANDA_ADMIN_PORT, SCHEMA_REGISTRY_PORT);
        withCreateContainerCmdModifier(cmd -> {
            cmd.withEntrypoint();
            cmd.withUser("root:root");
        });
        waitingFor(Wait.forLogMessage(".*Successfully started Redpanda!.*", 1));
        withCopyFileToContainer(
            MountableFile.forClasspathResource("testcontainers/entrypoint-tc.sh", 0700),
            "/entrypoint-tc.sh"
        );
        withCommand("/entrypoint-tc.sh", "redpanda", "start", "--mode=dev-container", "--smp=1", "--memory=1G");
    }

    @SneakyThrows
    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        super.containerIsStarting(containerInfo);

        Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        cfg.setDirectoryForTemplateLoading(new File(getClass().getResource("/testcontainers").getFile()));
        cfg.setDefaultEncoding("UTF-8");

        copyFileToContainer(getBootstrapFile(cfg), "/etc/redpanda/.bootstrap.yaml");
        copyFileToContainer(getRedpandaFile(cfg), "/etc/redpanda/redpanda.yaml");
    }

    public String getBootstrapServers() {
        return String.format("PLAINTEXT://%s:%s", getHost(), getMappedPort(REDPANDA_PORT));
    }

    public String getSchemaRegistryAddress() {
        return String.format("http://%s:%s", getHost(), getMappedPort(SCHEMA_REGISTRY_PORT));
    }

    public String getAdminAddress() {
        return String.format("http://%s:%s", getHost(), getMappedPort(REDPANDA_ADMIN_PORT));
    }

    public RedpandaContainer enableAuthorization() {
        this.enableAuthorization = true;
        return this;
    }

    public RedpandaContainer enableSasl() {
        this.authenticationMethod = "sasl";
        return this;
    }

    public RedpandaContainer enableSchemaRegistryHttpBasicAuth() {
        this.schemaRegistryAuthenticationMethod = "http_basic";
        return this;
    }

    public RedpandaContainer withSuperuser(String username) {
        this.superusers.add(username);
        return this;
    }

    private Transferable getBootstrapFile(Configuration cfg) {
        Map<String, Object> kafkaApi = new HashMap<>();
        kafkaApi.put("enableAuthorization", this.enableAuthorization);
        kafkaApi.put("superusers", this.superusers);

        Map<String, Object> root = new HashMap<>();
        root.put("kafkaApi", kafkaApi);

        String file = resolveTemplate(cfg, "bootstrap.yaml.ftl", root);

        return Transferable.of(file, 0700);
    }

    private Transferable getRedpandaFile(Configuration cfg) {
        Map<String, Object> kafkaApi = new HashMap<>();
        kafkaApi.put("authenticationMethod", this.authenticationMethod);
        kafkaApi.put("enableAuthorization", this.enableAuthorization);
        kafkaApi.put("advertisedHost", getHost());
        kafkaApi.put("advertisedPort", getMappedPort(9092));

        Map<String, Object> schemaRegistry = new HashMap<>();
        schemaRegistry.put("authenticationMethod", this.schemaRegistryAuthenticationMethod);

        Map<String, Object> root = new HashMap<>();
        root.put("kafkaApi", kafkaApi);
        root.put("schemaRegistry", schemaRegistry);

        String file = resolveTemplate(cfg, "redpanda.yaml.ftl", root);

        return Transferable.of(file, 0700);
    }

    @SneakyThrows
    private String resolveTemplate(Configuration cfg, String template, Map<String, Object> data) {
        Template temp = cfg.getTemplate(template);

        @Cleanup
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        @Cleanup
        Writer out = new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8);
        temp.process(data, out);

        return new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
    }
}
