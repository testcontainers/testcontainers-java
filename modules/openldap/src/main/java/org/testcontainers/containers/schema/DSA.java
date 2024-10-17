package org.testcontainers.containers.schema;

import lombok.Data;
import org.testcontainers.containers.schema.annotations.May;
import org.testcontainers.containers.schema.annotations.Oid;
import org.testcontainers.containers.schema.annotations.Rfc;
import org.testcontainers.containers.schema.annotations.RfcValue;

/**
 * RFC2256: a directory system agent (a server)
 */
@Data
@Rfc(RfcValue.RFC_2256)
@Oid(value = "2.5.6.13", description = "RFC2256: a directory system agent (a server)")
public class DSA extends ApplicationEntity {
    @May
    private String knowledgeInformation;
}
