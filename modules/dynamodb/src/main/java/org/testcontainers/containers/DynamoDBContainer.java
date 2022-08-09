package org.testcontainers.containers;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Testcontiners for DynamoDB
 *
 * @author Aran Moncusi
 */
@Slf4j
@SuppressWarnings("resource")
public class DynamoDBContainer extends GenericContainer<DynamoDBContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("amazon/dynamodb-local");

    private static final String DEFAULT_TAG = "1.18.0";

    private static final Integer DEFAULT_HTTP_PORT = 8000;

    private static final String DEFAULT_ACCESS_KEY = "DUMMYIDEXAMPLE";

    private static final String DEFAULT_SECRET_ACCESS_KEY = "DUMMYIDEXAMPLE";

    private String cors = "*";

    private Boolean flagSharedDb = false;

    private Boolean flagDelayTransientStatuses = false;

    private Boolean flagOptimizeDbBeforeStartup = false;

    private String dbPath;

    /**
     * Create a default DynamoDB Container using the official AWS DynamoDB docker image.
     *
     * @deprecated use {@link DynamoDBContainer(DockerImageName)} instead
     * @see DynamoDBContainer#DEFAULT_IMAGE_NAME
     * @see DynamoDBContainer#DEFAULT_TAG
     */
    @Deprecated
    public DynamoDBContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public DynamoDBContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withAccessKeys(DEFAULT_ACCESS_KEY, DEFAULT_SECRET_ACCESS_KEY);
        addExposedPort(DEFAULT_HTTP_PORT);

        waitingFor(
            Wait
                .forLogMessage(".*Initializing DynamoDB Local with the following configuration:.*", 1)
                .withStartupTimeout(Duration.ofSeconds(30))
        );
    }

    @Override
    protected void configure() {
        withCommand(getCommand());
    }

    private String[] getCommand() {
        final List<String> commands = new ArrayList<>(3);

        commands.add("-jar");
        commands.add("DynamoDBLocal.jar");

        commands.add("-port");
        commands.add(DEFAULT_HTTP_PORT.toString());

        commands.add("-cors");
        commands.add(cors);

        if (flagDelayTransientStatuses) {
            commands.add("-delayTransientStatuses");
        }

        if (flagSharedDb) {
            commands.add("-sharedDb");
        }

        if (flagOptimizeDbBeforeStartup) {
            commands.add("-optimizeDbBeforeStartup");
        }

        if (Objects.isNull(dbPath)) {
            commands.add("-inMemory");
        } else {
            commands.add("-dbPath");
            commands.add(dbPath);
        }

        return commands.toArray(new String[] {});
    }

    public Integer getPort() {
        return getMappedPort(DEFAULT_HTTP_PORT);
    }

    public DynamoDBContainer withAccessKeys(final String accessKey, final String secretKey) {
        withEnv("AWS_ACCESS_KEY_ID", accessKey);
        withEnv("AWS_SECRET_ACCESS_KEY", secretKey);

        return self();
    }

    public DynamoDBContainer withCors(final String cors) {
        this.cors = cors;

        return self();
    }

    public DynamoDBContainer withCors() {
        return withCors("*");
    }

    public DynamoDBContainer withCors(final String... domains) {
        return withCors(String.join(",", domains));
    }

    public DynamoDBContainer withFlagSharedDB(final Boolean isActiveFlag) {
        this.flagSharedDb = isActiveFlag;
        return self();
    }

    public DynamoDBContainer withEnableSharedDB() {
        withFlagSharedDB(true);
        return self();
    }

    public DynamoDBContainer withDisableSharedDB() {
        withFlagSharedDB(false);
        return self();
    }

    public DynamoDBContainer withFlagDelayTransientStatuses(final Boolean isActiveFlag) {
        this.flagDelayTransientStatuses = isActiveFlag;
        return self();
    }

    public DynamoDBContainer withEnableDelayTransientStatuses() {
        withFlagDelayTransientStatuses(true);
        return self();
    }

    public DynamoDBContainer withDisableDelayTransientStatuses() {
        withFlagDelayTransientStatuses(false);
        return self();
    }

    public DynamoDBContainer withFlagOptimizeDbBeforeStartup(final Boolean isActiveFlag) {
        this.flagOptimizeDbBeforeStartup = isActiveFlag;
        return self();
    }

    public DynamoDBContainer withEnableOptimizeDbBeforeStartup() {
        withFlagOptimizeDbBeforeStartup(true);
        return self();
    }

    public DynamoDBContainer withDisableOptimizeDbBeforeStartup() {
        withFlagOptimizeDbBeforeStartup(false);
        return self();
    }

    public DynamoDBContainer withInMemory() {
        this.dbPath = null;
        return self();
    }

    public DynamoDBContainer withFilePath(final String path) {
        this.dbPath = path;
        return self();
    }

    public DynamoDBContainer withFilePath() {
        return withFilePath(".");
    }

    @SuppressWarnings("HttpUrlsUsage")
    public String getEndpointUrl() {
        return "http://" + getHost() + ":" + getPort().toString();
    }
}
