package org.testcontainers.containers.schema;

import lombok.Data;
import org.testcontainers.containers.schema.annotations.May;
import org.testcontainers.containers.schema.annotations.Oid;
import org.testcontainers.containers.schema.annotations.Rfc;
import org.testcontainers.containers.schema.annotations.RfcValue;

/**
 * RFC2587: PKI certificate authority
 */
@Data
@Rfc(RfcValue.RFC_2587)
@Oid(value = "2.5.6.22", description = "RFC2587: PKI certificate authority")
public class PkiCA extends Top {

    @May
    private String authorityRevocationList;

    @May
    private String certificateRevocationList;

    @May
    private String cACertificate;

    @May
    private String crossCertificatePair;
}
