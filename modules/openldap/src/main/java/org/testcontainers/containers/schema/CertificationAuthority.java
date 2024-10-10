package org.testcontainers.containers.schema;

import lombok.Data;
import org.testcontainers.containers.schema.annotations.May;
import org.testcontainers.containers.schema.annotations.Must;
import org.testcontainers.containers.schema.annotations.Oid;
import org.testcontainers.containers.schema.annotations.Rfc;
import org.testcontainers.containers.schema.annotations.RfcValue;

/**
 * RFC2256: a certificate authorith
 */
@Data
@Oid(value = "2.5.6.16", description = "RFC2256: a certificate authority")
@Rfc(RfcValue.RFC_2256)
public class CertificationAuthority extends Top {
    public static final String DESCRIPTION = "RFC2256: a certificate authority";

    @Must
    private String authorityRevocationList;

    @Must
    private String certificateRevocationList;

    @Must
    private String cACertificate;

    @May
    private String crossCertificatePair;
}
