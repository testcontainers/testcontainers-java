package org.testcontainers.containers;

import lombok.SneakyThrows;
import org.apache.http.client.utils.URIBuilder;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AzuriteContainer extends GenericContainer<AzuriteContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "mcr.microsoft.com/azure-storage/azurite"
    );

    private static final String ACCOUNT_NAME = "devstoreaccount1";

    private static final String ACCOUNT_KEY =
        "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

    private final Set<AzuriteService> services = new HashSet<>();

    private final LogMessageWaitStrategy waitStrategy = new LogMessageWaitStrategy();

    public AzuriteContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        setWaitStrategy(waitStrategy);
    }

    @Override
    //@SneakyThrows
    protected void configure() {
        if (services.isEmpty()) {
            throw new IllegalStateException("At least one service must be specified");
        }
        services.forEach(service -> addExposedPort(service.getPort()));
    }

    public AzuriteContainer withService(AzuriteService service, AzuriteService... services) {
        this.services.add(service);
        this.services.addAll(Arrays.asList(services));
        return self();
    }

    @SuppressWarnings("SlowAbstractSetRemoveAll")
    public AzuriteContainer withoutService(AzuriteService service, AzuriteService... services) {
        this.services.remove(service);
        this.services.removeAll(Arrays.asList(services));
        return self();
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return services.stream().map(AzuriteService::getPort).collect(Collectors.toSet());
    }

    @Override
    protected void waitUntilContainerStarted() {
        if (services.contains(AzuriteService.BLOB)) {
            waitStrategy.withRegEx(".*Blob service is successfully listening.*");
        }
        if (services.contains(AzuriteService.QUEUE)) {
            waitStrategy.withRegEx(".*Queue service is successfully listening.*");
        }
        if (services.contains(AzuriteService.TABLE)) {
            waitStrategy.withRegEx(".*Table service is successfully listening.*");
        }
        waitStrategy.waitUntilReady(this);
    }

    public String getConnectionString() {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("DefaultEndpointsProtocol", "http");
        properties.put("AccountName", ACCOUNT_NAME);
        properties.put("AccountKey", ACCOUNT_KEY);
        properties.put("BlobEndpoint", getEndpoint(AzuriteService.BLOB));
        properties.put("QueueEndpoint", getEndpoint(AzuriteService.QUEUE));
        properties.put("TableEndpoint", getEndpoint(AzuriteService.TABLE));

        return properties
            .entrySet()
            .stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(";"));
    }

    public String getAccountName() {
        return ACCOUNT_NAME;
    }

    public String getAccountKey() {
        return ACCOUNT_KEY;
    }

    @SneakyThrows
    public String getEndpoint(AzuriteService service) {
        return new URIBuilder()
            .setScheme("http")
            .setHost(getHost())
            .setPort(getMappedPort(service.getPort()))
            .setPath(ACCOUNT_NAME)
            .build()
            .toASCIIString();
    }
}
