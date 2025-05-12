package org.testcontainers.redpanda;

import com.github.dockerjava.api.command.InspectContainerResponse;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Data;
import lombok.SneakyThrows;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Testcontainers implementation for Redpanda.
 * <p>
 * Supported images: {@code redpandadata/redpanda}, {@code docker.redpanda.com/redpandadata/redpanda}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Broker: 9092</li>
 *     <li>Schema Registry: 8081</li>
 *     <li>Proxy: 8082</li>
 * </ul>
 */
public class RedpandaContainer extends GenericContainer<RedpandaContainer> {

    private static final String REDPANDA_FULL_IMAGE_NAME = "docker.redpanda.com/redpandadata/redpanda";

    private static final String IMAGE_NAME = "redpandadata/redpanda";

    private static final DockerImageName REDPANDA_IMAGE = DockerImageName.parse(REDPANDA_FULL_IMAGE_NAME);

    private static final DockerImageName IMAGE = DockerImageName.parse(IMAGE_NAME);

    private static final int REDPANDA_PORT = 9092;

    private static final int REDPANDA_ADMIN_PORT = 9644;

    private static final int SCHEMA_REGISTRY_PORT = 8081;

    private static final int REST_PROXY_PORT = 8082;

    private boolean enableAuthorization;

    private String authenticationMethod = "none";

    private String schemaRegistryAuthenticationMethod = "none";

    private final List<String> superusers = new ArrayList<>();

    @Deprecated
    private final Set<Supplier<Listener>> listenersValueSupplier = new HashSet<>();

    private final Map<String, Supplier<String>> listeners = new HashMap<>();

    public RedpandaContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public RedpandaContainer(DockerImageName imageName) {
        super(imageName);
        imageName.assertCompatibleWith(REDPANDA_IMAGE, IMAGE);

        boolean isLessThanBaseVersion = new ComparableVersion(imageName.getVersionPart()).isLessThan("v22.2.1");
        boolean isPublicCompatibleImage =
            REDPANDA_FULL_IMAGE_NAME.equals(imageName.getUnversionedPart()) ||
            IMAGE_NAME.equals(imageName.getUnversionedPart());
        if (isPublicCompatibleImage && isLessThanBaseVersion) {
            throw new IllegalArgumentException("Redpanda version must be >= v22.2.1");
        }

        withExposedPorts(REDPANDA_PORT, REDPANDA_ADMIN_PORT, SCHEMA_REGISTRY_PORT, REST_PROXY_PORT);
        withCreateContainerCmdModifier(cmd -> {
            cmd.withEntrypoint("/entrypoint-tc.sh");
            cmd.withUser("root:root");
        });
        waitingFor(Wait.forLogMessage(".*Successfully started Redpanda!.*", 1));
        withCopyFileToContainer(
            MountableFile.forClasspathResource("testcontainers/entrypoint-tc.sh", 0700),
            "/entrypoint-tc.sh"
        );
        withCommand("redpanda", "start", "--mode=dev-container", "--smp=1", "--memory=1G");
    }

    @Override
    protected void configure() {
        this.listenersValueSupplier.stream()
            .map(Supplier::get)
            .map(Listener::getAddress)
            .forEach(this::withNetworkAliases);
        this.listeners.keySet().stream().map(listener -> listener.split(":")[0]).forEach(this::withNetworkAliases);
    }

    @SneakyThrows
    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        super.containerIsStarting(containerInfo);

        Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "testcontainers");
        cfg.setDefaultEncoding("UTF-8");

        copyFileToContainer(getBootstrapFile(cfg), "/etc/redpanda/.bootstrap.yaml");
        copyFileToContainer(getRedpandaFile(cfg), "/etc/redpanda/redpanda.yaml");
    }

    /**
     * Returns the bootstrap servers address.
     * @return the bootstrap servers address
     */
    public String getBootstrapServers() {
        return String.format("PLAINTEXT://%s:%s", getHost(), getMappedPort(REDPANDA_PORT));
    }

    /**
     * Returns the schema registry address.
     * @return the schema registry address
     */
    public String getSchemaRegistryAddress() {
        return String.format("http://%s:%s", getHost(), getMappedPort(SCHEMA_REGISTRY_PORT));
    }

    /**
     * Returns the admin address.
     * @return the admin address
     */
    public String getAdminAddress() {
        return String.format("http://%s:%s", getHost(), getMappedPort(REDPANDA_ADMIN_PORT));
    }

    /**
     * Returns the rest proxy address.
     * @return the rest proxy address
     */
    public String getRestProxyAddress() {
        return String.format("http://%s:%s", getHost(), getMappedPort(REST_PROXY_PORT));
    }

    /**
     * Enables authorization.
     * @return this {@link RedpandaContainer} instance
     */
    public RedpandaContainer enableAuthorization() {
        this.enableAuthorization = true;
        return this;
    }

    /**
     * Enables SASL.
     * @return this {@link RedpandaContainer} instance
     */
    public RedpandaContainer enableSasl() {
        this.authenticationMethod = "sasl";
        return this;
    }

    /**
     * Enables Http Basic Auth for Schema Registry.
     * @return this {@link RedpandaContainer} instance
     */
    public RedpandaContainer enableSchemaRegistryHttpBasicAuth() {
        this.schemaRegistryAuthenticationMethod = "http_basic";
        return this;
    }

    /**
     * Register username as a superuser.
     * @param username username to register as a superuser
     * @return this {@link RedpandaContainer} instance
     */
    public RedpandaContainer withSuperuser(String username) {
        this.superusers.add(username);
        return this;
    }

    /**
     * Add a {@link Supplier} that will provide a listener with format {@code host:port}.
     * Host will be added as a network alias.
     * <p>
     * The listener will be added to the default listeners.
     * <p>
     * Default listeners:
     * <ul>
     *     <li>0.0.0.0:9092</li>
     *     <li>0.0.0.0:9093</li>
     * </ul>
     * <p>
     * Default advertised listeners:
     * <ul>
     *      <li>{@code container.getHost():container.getMappedPort(9092)}</li>
     *      <li>127.0.0.1:9093</li>
     * </ul>
     * @param listenerSupplier a supplier that will provide a listener
     * @return this {@link RedpandaContainer} instance
     * @deprecated use {@link #withListener(String, Supplier)} instead
     */
    @Deprecated
    public RedpandaContainer withListener(Supplier<String> listenerSupplier) {
        String[] parts = listenerSupplier.get().split(":");
        this.listenersValueSupplier.add(() -> new Listener(parts[0], Integer.parseInt(parts[1])));
        return this;
    }

    /**
     * Add a listener in the format {@code host:port}.
     * Host will be included as a network alias.
     * <p>
     * Use it to register additional connections to the Kafka broker within the same container network.
     * <p>
     * The listener will be added to the list of default listeners.
     * <p>
     * Default listeners:
     * <ul>
     *     <li>0.0.0.0:9092</li>
     *     <li>0.0.0.0:9093</li>
     * </ul>
     * <p>
     * The listener will be added to the list of default advertised listeners.
     * <p>
     * Default advertised listeners:
     * <ul>
     *      <li>{@code container.getConfig().getHostName():9092}</li>
     *      <li>{@code container.getHost():container.getMappedPort(9093)}</li>
     * </ul>
     * @param listener a listener with format {@code host:port}
     * @return this {@link RedpandaContainer} instance
     */
    public RedpandaContainer withListener(String listener) {
        this.listeners.put(listener, () -> listener);
        return this;
    }

    /**
     * Add a listener in the format {@code host:port} and a {@link Supplier} for the advertised listener.
     * Host from listener will be included as a network alias.
     * <p>
     * Use it to register additional connections to the Kafka broker from outside the container network
     * <p>
     * The listener will be added to the list of default listeners.
     * <p>
     * Default listeners:
     * <ul>
     *     <li>0.0.0.0:9092</li>
     *     <li>0.0.0.0:9093</li>
     * </ul>
     * <p>
     * The {@link Supplier} will be added to the list of default advertised listeners.
     * <p>
     * Default advertised listeners:
     * <ul>
     *      <li>{@code container.getConfig().getHostName():9092}</li>
     *      <li>{@code container.getHost():container.getMappedPort(9093)}</li>
     * </ul>
     * @param listener a supplier that will provide a listener
     * @param advertisedListener a supplier that will provide a listener
     * @return this {@link RedpandaContainer} instance
     */
    public RedpandaContainer withListener(String listener, Supplier<String> advertisedListener) {
        this.listeners.put(listener, advertisedListener);
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

        List<Map<String, Object>> listeners =
            this.listenersValueSupplier.stream()
                .map(Supplier::get)
                .map(listener -> {
                    Map<String, Object> listenerMap = new HashMap<>();
                    listenerMap.put("address", listener.getAddress());
                    listenerMap.put("port", listener.getPort());
                    listenerMap.put("authentication_method", this.authenticationMethod);
                    return listenerMap;
                })
                .collect(Collectors.toList());
        kafkaApi.put("listeners", listeners);

        List<Map<String, Object>> kafkaListeners =
            this.listeners.keySet()
                .stream()
                .map(listener -> {
                    Map<String, Object> listenerMap = new HashMap<>();
                    listenerMap.put("name", listener.split(":")[0]);
                    listenerMap.put("address", listener.split(":")[0]);
                    listenerMap.put("port", listener.split(":")[1]);
                    listenerMap.put("authentication_method", this.authenticationMethod);
                    return listenerMap;
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> kafkaAdvertisedListeners =
            this.listeners.entrySet()
                .stream()
                .map(entry -> {
                    String advertisedListener = entry.getValue().get();
                    Map<String, Object> listenerMap = new HashMap<>();
                    listenerMap.put("name", entry.getKey().split(":")[0]);
                    listenerMap.put("address", advertisedListener.split(":")[0]);
                    listenerMap.put("port", advertisedListener.split(":")[1]);
                    return listenerMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> kafka = new HashMap<>();
        kafka.put("listeners", kafkaListeners);
        kafka.put("advertisedListeners", kafkaAdvertisedListeners);

        Map<String, Object> schemaRegistry = new HashMap<>();
        schemaRegistry.put("authenticationMethod", this.schemaRegistryAuthenticationMethod);

        Map<String, Object> root = new HashMap<>();
        root.put("kafkaApi", kafkaApi);
        root.put("kafka", kafka);
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

    @Data
    @AllArgsConstructor
    private static class Listener {

        private String address;

        private int port;
    }
}
