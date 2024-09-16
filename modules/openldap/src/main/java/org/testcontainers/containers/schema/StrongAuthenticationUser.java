package org.testcontainers.containers.schema;

import org.testcontainers.containers.schema.annotations.Oid;
import org.testcontainers.containers.schema.annotations.Rfc;
import org.testcontainers.containers.schema.annotations.RfcValue;

/**
 * RFC2256: a strong authentication user
 */
@Rfc(RfcValue.RFC_2256)
@Oid(value = "2.5.6.15", description = "RFC2256: a strong authentication user")
public interface StrongAuthenticationUser {

    // MUST
    String getUserCertificate();

    void setUserCertificate(String userCertificate);
}
