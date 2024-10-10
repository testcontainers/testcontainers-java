package org.testcontainers.containers.schema;

import org.testcontainers.containers.schema.annotations.Oid;
import org.testcontainers.containers.schema.annotations.Rfc;
import org.testcontainers.containers.schema.annotations.RfcValue;

/**
 * RFC2256: a user security information
 */
@Rfc(RfcValue.RFC_2256)
@Oid(value = "2.5.6.18", description = "RFC2256: a user security information")
public interface UserSecurityInformation {

    // MAY
    String getSupportedAlgorithms();

    void setSupportedAlgorithms(String algorithms);
}
