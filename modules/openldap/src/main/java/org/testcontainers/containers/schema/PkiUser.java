package org.testcontainers.containers.schema;

import org.testcontainers.containers.schema.annotations.Oid;
import org.testcontainers.containers.schema.annotations.Rfc;
import org.testcontainers.containers.schema.annotations.RfcValue;

/**
 * RFC2587: a PKI user
 */
@Rfc(RfcValue.RFC_2587)
@Oid(value = "2.5.6.21", description = "RFC2587: a PKI user")
public interface PkiUser {

    // MAY
    String getUserCertificate();

    void setUserCertificate(String userCertificate);
}
