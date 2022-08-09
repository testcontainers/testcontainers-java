package org.testcontainers.containers;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Testcontainers for AWS DynamoDB
 *
 * @author Aran Moncusi
 */
@Slf4j
@SuppressWarnings("resource")
public class DynamoDBContainer extends GenericContainer<DynamoDBContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("amazon/dynamodb-local");

    private static final String DEFAULT_TAG = "1.18.0";

    private static final Integer DEFAULT_HTTP_PORT = 8000;

    private String cors = "*";

    private Boolean flagSharedDb = false;

    private Boolean flagDelayTransientStatuses = false;

    private Boolean flagOptimizeDbBeforeStartup = false;

    private Boolean flagInMemory = false;

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

    public DynamoDBContainer(final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public DynamoDBContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

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
            Preconditions.checkState(
                Objects.nonNull(dbPath),
                "If you use the 'optimizeDbBeforeStartup' option, you must also specify the 'dbPath' parameter so " +
                "that DynamoDB can find its database file."
            );

            commands.add("-optimizeDbBeforeStartup");
        }

        if (flagInMemory) {
            Preconditions.checkState(Objects.isNull(dbPath), "You can't specify both dbPath and inMemory at once.");

            commands.add("-inMemory");
        }

        if (Objects.nonNull(dbPath)) {
            commands.add("-dbPath");
            commands.add(dbPath);
        }

        return commands.toArray(new String[] {});
    }

    /**
     * Get current port for HTTP DynamoDB API
     *
     * @return Current mapped port, never null.
     */
    @NonNull
    public Integer getPort() {
        return getMappedPort(DEFAULT_HTTP_PORT);
    }

    /**
     * Specify the allowed domains for CORS in a comma-separated "raw" format. These values are put directly in the
     * '-port' option.
     *
     * @param cors Raw string with allowed domains.
     *
     * @return self instance.
     * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.UsageNotes.html#DynamoDBLocal.CommandLineOptions">AWS DynamoDB Docs for Downloaded version</a>
     */
    public DynamoDBContainer withCors(final String cors) {
        this.cors = cors;
        return self();
    }

    /**
     * Default CORS value to allow all domains.
     *
     * @return self instance.
     * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.UsageNotes.html#DynamoDBLocal.CommandLineOptions">AWS DynamoDB Docs for Downloaded version</a>
     */
    public DynamoDBContainer withCors() {
        return withCors("*");
    }

    /**
     * Array list of allowed domains for CORS.
     *
     * @param domains List of allowed domains for CORS.
     *
     * @return self instance.
     * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.UsageNotes.html#DynamoDBLocal.CommandLineOptions">AWS DynamoDB Docs for Downloaded version</a>
     */
    public DynamoDBContainer withCors(final String... domains) {
        return withCors(String.join(",", domains));
    }

    /**
     * Set the flag '-sharedDb' on container commands.
     *
     * @return self instance.
     * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.UsageNotes.html#DynamoDBLocal.CommandLineOptions">AWS DynamoDB Docs for Downloaded version</a>
     */
    public DynamoDBContainer withFlagSharedDB(final Boolean isActiveFlag) {
        this.flagSharedDb = isActiveFlag;
        return self();
    }

    /**
     * Enable the flag sharedDb;
     *
     * @return self instance.
     * @see #withFlagSharedDB(Boolean)
     */
    public DynamoDBContainer withEnableSharedDB() {
        withFlagSharedDB(true);
        return self();
    }

    /**
     * Disable the flag sharedDb;
     *
     * @return self instance.
     * @see #withFlagSharedDB(Boolean)
     */
    public DynamoDBContainer withDisableSharedDB() {
        withFlagSharedDB(false);
        return self();
    }

    /**
     * Set the flag '-delayTransientStatuses' on container commands.
     *
     * @return self instance.
     * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.UsageNotes.html#DynamoDBLocal.CommandLineOptions">AWS DynamoDB Docs for Downloaded version</a>
     */
    public DynamoDBContainer withFlagDelayTransientStatuses(final Boolean isActiveFlag) {
        this.flagDelayTransientStatuses = isActiveFlag;
        return self();
    }

    /**
     * Enable the flag delayTransientStatuses;
     *
     * @return self instance.
     * @see #withFlagDelayTransientStatuses(Boolean)
     */
    public DynamoDBContainer withEnableDelayTransientStatuses() {
        withFlagDelayTransientStatuses(true);
        return self();
    }

    /**
     * Disable the flag delayTransientStatuses;
     *
     * @return self instance.
     * @see #withFlagDelayTransientStatuses(Boolean)
     */
    public DynamoDBContainer withDisableDelayTransientStatuses() {
        withFlagDelayTransientStatuses(false);
        return self();
    }

    /**
     * Set the flag '-optimizeDbBeforeStartup' on container commands.
     *
     * @return self instance.
     * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.UsageNotes.html#DynamoDBLocal.CommandLineOptions">AWS DynamoDB Docs for Downloaded version</a>
     */
    public DynamoDBContainer withFlagOptimizeDbBeforeStartup(final Boolean isActiveFlag) {
        this.flagOptimizeDbBeforeStartup = isActiveFlag;
        return self();
    }

    /**
     * Enable the flag optimizeDbBeforeStartup;
     *
     * @return self instance.
     * @see #withFlagOptimizeDbBeforeStartup(Boolean)
     */
    public DynamoDBContainer withEnableOptimizeDbBeforeStartup() {
        withFlagOptimizeDbBeforeStartup(true);
        return self();
    }

    /**
     * Disable the flag optimizeDbBeforeStartup;
     *
     * @return self instance.
     * @see #withFlagOptimizeDbBeforeStartup(Boolean)
     */
    public DynamoDBContainer withDisableOptimizeDbBeforeStartup() {
        withFlagOptimizeDbBeforeStartup(false);
        return self();
    }

    /**
     * Enable '-inMemory' flag and set null dbPath attribute.
     *
     * @return self instance.
     * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.UsageNotes.html#DynamoDBLocal.CommandLineOptions">AWS DynamoDB Docs for Downloaded version</a>
     */
    public DynamoDBContainer withInMemory() {
        this.flagInMemory = true;
        this.dbPath = null;
        return self();
    }

    /**
     * Disable '-inMemory' flag and set the dbPath attribute.
     *
     * @param path Expected dbPath. Nullable values puts the default value.
     *
     * @return self instance.
     * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.UsageNotes.html#DynamoDBLocal.CommandLineOptions">AWS DynamoDB Docs for Downloaded version</a>
     */
    public DynamoDBContainer withFilePath(final String path) {
        this.flagInMemory = false;
        this.dbPath = path;
        return self();
    }

    /**
     * Disable '-inMemory' flag and set default value of dbPath attribute.
     *
     * @return self instance.
     * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.UsageNotes.html#DynamoDBLocal.CommandLineOptions">AWS DynamoDB Docs for Downloaded version</a>
     */
    public DynamoDBContainer withFilePath() {
        return withFilePath(null);
    }

    /**
     * Build a URL allowed for AWS SDK parameter '--endpoint' attribute.
     *
     * @return HTTP URL for current instance.
     * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.UsageNotes.html#DynamoDBLocal.Endpoint">AWS DynamoDB Docs for Downloaded version</a>
     */
    @SuppressWarnings("HttpUrlsUsage")
    public String getEndpointUrl() {
        return "http://" + getHost() + ":" + getPort().toString();
    }
}
