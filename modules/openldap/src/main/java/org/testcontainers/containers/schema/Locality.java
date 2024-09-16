package org.testcontainers.containers.schema;

import lombok.Data;
import org.testcontainers.containers.schema.annotations.May;
import org.testcontainers.containers.schema.annotations.Oid;
import org.testcontainers.containers.schema.annotations.Rfc;
import org.testcontainers.containers.schema.annotations.RfcValue;

/**
 * RFC2256: a locality
 */
@Data
@Rfc(RfcValue.RFC_2256)
@Oid(value = "2.5.6.3", description = "RFC2256: a locality")
public class Locality extends Top {

    @May
    private String street;

    @May
    private String seeAlso;

    @May
    private String searchGuide;

    @May
    private String st;

    @May
    private String l;

    @May
    private String description;;
}
