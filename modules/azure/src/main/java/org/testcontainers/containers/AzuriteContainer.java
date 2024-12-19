package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Testcontainers implementation for Azurite Emulator.
 * <p>
 * Supported image: {@code mcr.microsoft.com/azure-storage/azurite}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>10000 (default blob port, configurable)</li>
 *     <li>10001 (default queue port, configurable)</li>
 *     <li>10002 (default table port, configurable)</li>
 * </ul>
 * <p>
 * See command line options <a href="https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite?tabs=visual-studio%2Cblob-storage#command-line-options">here</a>.
 */
public class AzuriteContainer extends GenericContainer<AzuriteContainer> {

    static final String DEFAULT_HOST = "127.0.0.1";

    static final int DEFAULT_BLOB_PORT = 10000;

    static final int DEFAULT_QUEUE_PORT = 10001;

    static final int DEFAULT_TABLE_PORT = 10002;

    static final String DEFAULT_LOCATION = "/data";

    /**
     * The account name of the default credentials.
     */
    public static final String WELL_KNOWN_ACCOUNT_NAME = "devstoreaccount1";

    /**
     * The account key of the default credentials.
     */
    public static final String WELL_KNOWN_ACCOUNT_KEY =
        "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

    static final int NO_LIMIT = 0;

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "mcr.microsoft.com/azure-storage/azurite"
    );

    private boolean useBlob = true;

    private String blobHost = DEFAULT_HOST;

    private int blobPort = DEFAULT_BLOB_PORT;

    private boolean useQueue = true;

    private String queueHost = DEFAULT_HOST;

    private int queuePort = DEFAULT_QUEUE_PORT;

    private boolean useTable = true;

    private String tableHost = DEFAULT_HOST;

    private int tablePort = DEFAULT_TABLE_PORT;

    private String location = DEFAULT_LOCATION;

    private boolean silent = false;

    private String debug = null;

    private boolean loose = false;

    private boolean version = false;

    private File cert = null;

    private String certExtension = null;

    private File key = null;

    private String pwd = null;

    private String oauth = null;

    private boolean skipApiVersionCheck = false;

    private boolean disableProductStyleUrl = false;

    private boolean inMemoryPersistence = false;

    private long extentMemoryLimit = NO_LIMIT;

    private final List<String> azuriteAccounts = new ArrayList<>();

    /**
     * @param dockerImageName specified docker image name to run
     */
    public AzuriteContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    }

    /**
     * Configure SSL with a custom certificate and password.
     *
     * @param pfxCert  The PFX certificate file
     * @param password The password securing the certificate
     * @return this
     */
    public AzuriteContainer withSsl(final File pfxCert, final String password) {
        cert = pfxCert;
        pwd = password;
        certExtension = ".pfx";
        return this;
    }

    /**
     * Configure SSL with a custom certificate and private key.
     *
     * @param pemCert The PEM certificate file
     * @param pemKey  The PEM key file
     * @return this
     */
    public AzuriteContainer withSsl(final File pemCert, final File pemKey) {
        cert = pemCert;
        key = pemKey;
        certExtension = ".pem";
        return this;
    }

    /**
     * Disable Blob functionality.
     *
     * @return this
     */
    public AzuriteContainer withoutBlob() {
        this.useBlob = false;
        return this;
    }

    /**
     * Disables Queue functionality.
     *
     * @return this
     */
    public AzuriteContainer withoutQueue() {
        this.useQueue = false;
        return this;
    }

    /**
     * Disables Table functionality.
     *
     * @return this
     */
    public AzuriteContainer withoutTable() {
        this.useTable = false;
        return this;
    }

    /**
     * Sets the hostname we want to use to connect to our emulator. (default: {@link #DEFAULT_HOST})
     *
     * @param host The host name
     * @return this
     */
    public AzuriteContainer withHost(final String host) {
        blobHost = host;
        queueHost = host;
        tableHost = host;
        return this;
    }

    /**
     * Sets the container port we want to use for the Blob functionality. (default: {@link #DEFAULT_BLOB_PORT})
     *
     * @param port The Blob port
     * @return this
     */
    public AzuriteContainer withBlobPort(final int port) {
        blobPort = port;
        return this;
    }

    /**
     * Sets the container port we want to use for the Queue functionality. (default: {@link #DEFAULT_QUEUE_PORT})
     *
     * @param port The Queue port
     * @return this
     */
    public AzuriteContainer withQueuePort(final int port) {
        queuePort = port;
        return this;
    }

    /**
     * Sets the container port we want to use for the Table functionality. (default: {@link #DEFAULT_TABLE_PORT})
     *
     * @param port The Table port
     * @return this
     */
    public AzuriteContainer withTablePort(final int port) {
        tablePort = port;
        return this;
    }

    /**
     * Sets the host name and the container port we want to use for the Blob functionality.
     * (default: {@link #DEFAULT_HOST} and {@link #DEFAULT_BLOB_PORT})
     *
     * @param host The host name
     * @param port The Blob port
     * @return this
     */
    public AzuriteContainer withBlobEndpoint(final String host, final int port) {
        blobHost = host;
        blobPort = port;
        return this;
    }

    /**
     * Sets the host name and the container port we want to use for the Queue functionality.
     * (default: {@link #DEFAULT_HOST} and {@link #DEFAULT_QUEUE_PORT})
     *
     * @param host The host name
     * @param port The Queue port
     * @return this
     */
    public AzuriteContainer withQueueEndpoint(final String host, final int port) {
        queueHost = host;
        queuePort = port;
        return this;
    }

    /**
     * Sets the host name and the container port we want to use for the Table functionality.
     * (default: {@link #DEFAULT_HOST} and {@link #DEFAULT_TABLE_PORT})
     *
     * @param host The host name
     * @param port The Table port
     * @return this
     */
    public AzuriteContainer withTableEndpoint(final String host, final int port) {
        tableHost = host;
        tablePort = port;
        return this;
    }

    /**
     * Sets the file system location where the data should be stored in the container.
     * (default: {@link #DEFAULT_LOCATION})
     *
     * @param location The file system location.
     * @return this
     */
    public AzuriteContainer withLocation(final String location) {
        this.location = location;
        return this;
    }

    /**
     * Tells Azurite to use silent mode.
     *
     * @return this
     */
    public AzuriteContainer withSilentMode() {
        this.silent = true;
        return this;
    }

    /**
     * Defines the file system path where the debug logs should be saved in the container.
     *
     * @param debugLog The path of the debug log.
     * @return this
     */
    public AzuriteContainer withDebugLog(final String debugLog) {
        this.debug = debugLog;
        return this;
    }

    /**
     * Tells Azurite to use loose mode.
     *
     * @return this
     */
    public AzuriteContainer withLooseMode() {
        this.loose = true;
        return this;
    }

    /**
     * Tells Azurite to print the version to the output.
     *
     * @return this
     */
    public AzuriteContainer withPrintVersion() {
        this.version = true;
        return this;
    }

    /**
     * Enables OAuth authentication. Requires SSL as well.
     * {@link #withSsl(File, String)}
     * {@link #withSsl(File, File)}
     *
     * @param oauth The OAuth parameter
     * @return this
     */
    public AzuriteContainer withOauth(final String oauth) {
        this.oauth = oauth;
        return this;
    }

    /**
     * Tells Azurite to skip checking the API version.
     *
     * @return this
     */
    public AzuriteContainer withSkipApiVersionCheck() {
        this.skipApiVersionCheck = true;
        return this;
    }

    /**
     * Tells Azurite to disable product style URLs.
     *
     * @return this
     */
    public AzuriteContainer withDisableProductStyleUrl() {
        this.disableProductStyleUrl = true;
        return this;
    }

    /**
     * Tells Azurite to store content in memory using the default limits.
     *
     * @return this
     */
    public AzuriteContainer withInMemoryPersistence() {
        this.inMemoryPersistence = true;
        return this;
    }

    /**
     * Tells Azurite to store content in memory using a custom limit.
     *
     * @param limit The memory limit in MBs
     * @return this
     */
    public AzuriteContainer withInMemoryPersistence(final long limit) {
        this.inMemoryPersistence = true;
        this.extentMemoryLimit = limit;
        return this;
    }

    /**
     * Adds an account name and account key to be able to use them for authentication.
     * (default name: {@link #WELL_KNOWN_ACCOUNT_NAME})
     * (default key: {@link #WELL_KNOWN_ACCOUNT_KEY})
     *
     * @param accountName The name of the account
     * @param primaryKey  The account key
     * @return this
     */
    public AzuriteContainer addAccountCredentials(final String accountName, final String primaryKey) {
        return addAccountCredentials(accountName, primaryKey, null);
    }

    /**
     * Adds an account name and account keys to be able to use them for authentication.
     * (default name: {@link #WELL_KNOWN_ACCOUNT_NAME})
     * (default key: {@link #WELL_KNOWN_ACCOUNT_KEY})
     *
     * @param accountName  The name of the account
     * @param primaryKey   The primary account key
     * @param secondaryKey The secondary account key (optional)
     * @return this
     */
    public AzuriteContainer addAccountCredentials(
        final String accountName,
        final String primaryKey,
        final String secondaryKey
    ) {
        final StringBuilder credentialBuilder = new StringBuilder().append(accountName).append(":").append(primaryKey);
        Optional.ofNullable(secondaryKey).ifPresent(s -> credentialBuilder.append(":").append(s));
        this.azuriteAccounts.add(credentialBuilder.toString());
        return this;
    }

    @Override
    protected void configure() {
        super.configure();
        withEnv("AZURITE_ACCOUNTS", getAccounts());
        withCommand(getCommandLine());
        if (cert != null) {
            final String certAbsolutePath = cert.getAbsolutePath();
            logger().info("Using path for cert file: '{}'", certAbsolutePath);
            withFileSystemBind(certAbsolutePath, "/cert" + certExtension, BindMode.READ_ONLY);
            if (key != null) {
                final String keyAbsolutePath = key.getAbsolutePath();
                logger().info("Using path for key file: '{}'", keyAbsolutePath);
                withFileSystemBind(keyAbsolutePath, "/key.pem", BindMode.READ_ONLY);
            }
        }
        exposeRelevantPorts();
    }

    /**
     * Returns the connection string for the default credentials.
     *
     * @return connection string
     */
    public String getDefaultConnectionString() {
        return getConnectionString(WELL_KNOWN_ACCOUNT_NAME, WELL_KNOWN_ACCOUNT_KEY);
    }

    /**
     * Returns the connection string for the account name and key specified.
     *
     * @param accountName The name of the account
     * @param accountKey  The account key
     * @return connection string
     */
    public String getConnectionString(final String accountName, final String accountKey) {
        return new AzuriteConnectionStringBuilder()
            .accountCredentials(accountName, accountKey)
            .useSsl(cert != null)
            .blobEndpoint(blobHost, getPortNumberFor(blobPort), useBlob)
            .queueEndpoint(queueHost, getPortNumberFor(queuePort), useQueue)
            .tableEndpoint(tableHost, getPortNumberFor(tablePort), useTable)
            .build();
    }

    String getCommandLine() {
        final StringBuilder args = new StringBuilder(getExecutableName());
        if (useBlob) {
            args.append(" --blobHost ").append(blobHost).append(" --blobPort ").append(blobPort);
        }
        if (useQueue) {
            args.append(" --queueHost ").append(queueHost).append(" --queuePort ").append(queuePort);
        }
        if (useTable) {
            args.append(" --tableHost ").append(tableHost).append(" --tablePort ").append(tablePort);
        }
        if (silent) {
            args.append(" --silent");
        }
        if (version) {
            args.append(" --version");
        }
        if (debug != null) {
            args.append(" --debug ").append(debug);
        }
        if (loose) {
            args.append(" --loose");
        }
        if (disableProductStyleUrl) {
            args.append(" --disableProductStyleUrl");
        }
        if (skipApiVersionCheck) {
            args.append(" --skipApiVersionCheck");
        }
        if (inMemoryPersistence) {
            args.append(" --inMemoryPersistence");
            if (extentMemoryLimit > NO_LIMIT) {
                args.append(" --extentMemoryLimit ").append(extentMemoryLimit);
            }
        } else {
            args.append(" --location ").append(location);
        }
        if (cert != null) {
            args.append(" --cert ").append("/cert").append(certExtension);
            if (pwd != null) {
                args.append(" --pwd ").append(pwd);
            } else {
                args.append(" --key ").append("/key.pem");
            }
            if (oauth != null) {
                args.append(" --oauth ").append(oauth);
            }
        }
        final String cmd = args.toString();
        logger().debug("Using command line: '{}'", cmd);
        return cmd;
    }

    private void exposeRelevantPorts() {
        if (useBlob) {
            addExposedPort(blobPort);
        }
        if (useQueue) {
            addExposedPort(queuePort);
        }
        if (useTable) {
            addExposedPort(tablePort);
        }
    }

    private int getPortNumberFor(final int port) {
        int mappedPort = port;
        if (getExposedPorts().contains(port)) {
            mappedPort = getMappedPort(port);
        }
        return mappedPort;
    }

    private String getAccounts() {
        return String.join(";", azuriteAccounts);
    }

    private String getExecutableName() {
        String executable = "azurite";
        if (useBlob && !useQueue && !useTable) {
            executable = "azurite-blob";
        } else if (!useBlob && useQueue && !useTable) {
            executable = "azurite-queue";
        } else if (!useBlob && !useQueue && useTable) {
            executable = "azurite-table";
        }
        return executable;
    }
}
