package org.testcontainers.containers.schema;

import lombok.Data;
import org.testcontainers.containers.schema.annotations.May;
import org.testcontainers.containers.schema.annotations.Must;
import org.testcontainers.containers.schema.annotations.Oid;
import org.testcontainers.containers.schema.annotations.Rfc;
import org.testcontainers.containers.schema.annotations.RfcValue;

/**
 * RFC2256: an application process
 */
@Data
@Rfc(RfcValue.RFC_2256)
@Oid(value = "2.5.6.11", description = "RFC2256: an application process")
public class ApplicationProcess extends Top {
    @Must
    private String cn;

    @May
    private String seeAlso;
    @May
    private String ou;
    @May
    private String l;
    @May
    private String description;
}
