package org.testcontainers.azure;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

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
    private static final String WELL_KNOWN_ACCOUNT_NAME = "devstoreaccount1";

    /**
     * The account key of the default credentials.
     */
    private static final String WELL_KNOWN_ACCOUNT_KEY =
        "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "mcr.microsoft.com/azure-storage/azurite"
    );

    private String host = DEFAULT_HOST;

    private MountableFile cert = null;

    private String certExtension = null;

    private MountableFile key = null;

    private String pwd = null;

    /**
     * @param dockerImageName specified docker image name to run
     */
    public AzuriteContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(DEFAULT_BLOB_PORT, DEFAULT_QUEUE_PORT, DEFAULT_TABLE_PORT);
    }

    /**
     * Configure SSL with a custom certificate and password.
     *
     * @param pfxCert  The PFX certificate file
     * @param password The password securing the certificate
     * @return this
     */
    public AzuriteContainer withSsl(final MountableFile pfxCert, final String password) {
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
    public AzuriteContainer withSsl(final MountableFile pemCert, final MountableFile pemKey) {
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
        withCommand(getCommandLine());
        if (cert != null) {
            logger().info("Using path for cert file: '{}'", cert);
            withCopyFileToContainer(cert, "/cert" + certExtension);
            if (key != null) {
                logger().info("Using path for key file: '{}'", key);
                withCopyFileToContainer(key, "/key.pem");
            }
        }
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
            getHost(),
            getMappedPort(DEFAULT_BLOB_PORT),
            accountName,
            protocol,
            getHost(),
            getMappedPort(DEFAULT_QUEUE_PORT),
            accountName,
            protocol,
            getHost(),
            getMappedPort(DEFAULT_TABLE_PORT),
            accountName
        );
    }

    String getCommandLine() {
        final StringBuilder args = new StringBuilder("azurite");
        args.append(" --blobHost ").append(ALLOW_ALL_CONNECTIONS);
        args.append(" --queueHost ").append(ALLOW_ALL_CONNECTIONS);
        args.append(" --tableHost ").append(ALLOW_ALL_CONNECTIONS);
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
