package org.testcontainers.containers;

import org.rnorth.ducttape.Preconditions;

/**
 * Builds connection strings for the {@link AzuriteContainer}.
 */
class AzuriteConnectionStringBuilder {

    private static final String CONNECTION_PROTOCOL_FORMAT = "DefaultEndpointsProtocol=%s;";

    private static final String ACCOUNT_DETAILS_FORMAT = "AccountName=%s;AccountKey=%s;";

    private static final String BLOB_ENDPOINT_FORMAT = "BlobEndpoint=%s://%s:%d/%s;";

    private static final String QUEUE_ENDPOINT_FORMAT = "QueueEndpoint=%s://%s:%d/%s;";

    private static final String TABLE_ENDPOINT_FORMAT = "TableEndpoint=%s://%s:%d/%s;";

    private String protocol = "http";

    private String accountName = AzuriteContainer.WELL_KNOWN_ACCOUNT_NAME;

    private String accountKey = AzuriteContainer.WELL_KNOWN_ACCOUNT_KEY;

    private boolean useBlob = true;

    private String blobHost = AzuriteContainer.DEFAULT_HOST;

    private int blobPort = AzuriteContainer.DEFAULT_BLOB_PORT;

    private boolean useQueue = true;

    private String queueHost = AzuriteContainer.DEFAULT_HOST;

    private int queuePort = AzuriteContainer.DEFAULT_QUEUE_PORT;

    private boolean useTable = true;

    private String tableHost = AzuriteContainer.DEFAULT_HOST;

    private int tablePort = AzuriteContainer.DEFAULT_TABLE_PORT;

    AzuriteConnectionStringBuilder useSsl(final boolean useSsl) {
        protocol = useSsl ? "https" : "http";
        return this;
    }

    AzuriteConnectionStringBuilder accountCredentials(final String accountName, final String accountKey) {
        this.accountName = accountName;
        this.accountKey = accountKey;
        return this;
    }

    AzuriteConnectionStringBuilder blobEndpoint(final String host, final int port, final boolean enabled) {
        blobHost = host;
        blobPort = port;
        useBlob = enabled;
        return this;
    }

    AzuriteConnectionStringBuilder queueEndpoint(final String host, final int port, final boolean enabled) {
        queueHost = host;
        queuePort = port;
        useQueue = enabled;
        return this;
    }

    AzuriteConnectionStringBuilder tableEndpoint(final String host, final int port, final boolean enabled) {
        tableHost = host;
        tablePort = port;
        useTable = enabled;
        return this;
    }

    String build() {
        Preconditions.check("At least one of the blob, queue or table must be used!", useBlob || useQueue || useTable);

        final StringBuilder stringBuilder = new StringBuilder()
            .append(String.format(CONNECTION_PROTOCOL_FORMAT, protocol))
            .append(String.format(ACCOUNT_DETAILS_FORMAT, accountName, accountKey));
        if (useBlob) {
            stringBuilder.append(String.format(BLOB_ENDPOINT_FORMAT, protocol, blobHost, blobPort, accountName));
        }
        if (useQueue) {
            stringBuilder.append(String.format(QUEUE_ENDPOINT_FORMAT, protocol, queueHost, queuePort, accountName));
        }
        if (useTable) {
            stringBuilder.append(String.format(TABLE_ENDPOINT_FORMAT, protocol, tableHost, tablePort, accountName));
        }
        return stringBuilder.toString();
    }
}
