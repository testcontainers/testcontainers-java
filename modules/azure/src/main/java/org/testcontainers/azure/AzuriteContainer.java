package org.testcontainers.azure;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;

/**
 * Testcontainers implementation for Azurite Emulator.
 * <p>
 * Supported image: {@code mcr.microsoft.com/azure-storage/azurite}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>10000 (blob port)</li>
 *     <li>10001 (queue port)</li>
 *     <li>10002 (table port)</li>
 * </ul>
 * <p>
 * See command line options <a href="https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite?tabs=visual-studio%2Cblob-storage#command-line-options">here</a>.
 */
public class AzuriteContainer extends GenericContainer<AzuriteContainer> {

    private static final String DEFAULT_HOST = "127.0.0.1";

    private static final String ALLOW_ALL_CONNECTIONS = "0.0.0.0";

    private static final int DEFAULT_BLOB_PORT = 10000;

    private static final int DEFAULT_QUEUE_PORT = 10001;

    private static final int DEFAULT_TABLE_PORT = 10002;

    private static final String CONNECTION_STRING_FORMAT =
        "DefaultEndpointsProtocol=%s;AccountName=%s;AccountKey=%s;BlobEndpoint=%s://%s:%d/%s;QueueEndpoint=%s://%s:%d/%s;TableEndpoint=%s://%s:%d/%s;";

    /**
     * The account name of the default credentials.
     */
    public static final String WELL_KNOWN_ACCOUNT_NAME = "devstoreaccount1";

    /**
     * The account key of the default credentials.
     */
    public static final String WELL_KNOWN_ACCOUNT_KEY =
        "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "mcr.microsoft.com/azure-storage/azurite"
    );

    private String host = DEFAULT_HOST;

    private File cert = null;

    private String certExtension = null;

    private File key = null;

    private String pwd = null;

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
     * Sets the hostname we want to use to connect to our emulator. (default: {@link #DEFAULT_HOST})
     *
     * @param host The host name
     * @return this
     */
    public AzuriteContainer withHost(final String host) {
        this.host = host;
        return this;
    }

    @Override
    protected void configure() {
        withEnv("AZURITE_ACCOUNTS", WELL_KNOWN_ACCOUNT_NAME + ":" + WELL_KNOWN_ACCOUNT_KEY);
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
        withExposedPorts(DEFAULT_BLOB_PORT, DEFAULT_QUEUE_PORT, DEFAULT_TABLE_PORT);
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
        final String protocol = cert != null ? "https" : "http";
        return String.format(
            CONNECTION_STRING_FORMAT,
            protocol,
            accountName,
            accountKey,
            protocol,
            host,
            getMappedPort(DEFAULT_BLOB_PORT),
            accountName,
            protocol,
            host,
            getMappedPort(DEFAULT_QUEUE_PORT),
            accountName,
            protocol,
            host,
            getMappedPort(DEFAULT_TABLE_PORT),
            accountName
        );
    }

    String getCommandLine() {
        final StringBuilder args = new StringBuilder("azurite");
        args.append(" --blobHost ").append(ALLOW_ALL_CONNECTIONS).append(" --blobPort ").append(DEFAULT_BLOB_PORT);
        args.append(" --queueHost ").append(ALLOW_ALL_CONNECTIONS).append(" --queuePort ").append(DEFAULT_QUEUE_PORT);
        args.append(" --tableHost ").append(ALLOW_ALL_CONNECTIONS).append(" --tablePort ").append(DEFAULT_TABLE_PORT);
        args.append(" --location ").append("/data");
        if (cert != null) {
            args.append(" --cert ").append("/cert").append(certExtension);
            if (pwd != null) {
                args.append(" --pwd ").append(pwd);
            } else {
                args.append(" --key ").append("/key.pem");
            }
        }
        final String cmd = args.toString();
        logger().debug("Using command line: '{}'", cmd);
        return cmd;
    }
}
