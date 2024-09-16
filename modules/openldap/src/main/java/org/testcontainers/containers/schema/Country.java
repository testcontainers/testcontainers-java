package org.testcontainers.containers.schema;

import lombok.Data;
import org.testcontainers.containers.schema.annotations.May;
import org.testcontainers.containers.schema.annotations.Must;
import org.testcontainers.containers.schema.annotations.Oid;
import org.testcontainers.containers.schema.annotations.Rfc;
import org.testcontainers.containers.schema.annotations.RfcValue;

/**
 * RFC2256: a country
 */
@Data
@Rfc(RfcValue.RFC_2256)
@Oid(value = "2.5.6.2", description = "RFC2256: a country")
public class Country extends Top {

    @Must
    private String c;

    @May
    private String searchGuide;

    @May
    private String description;
}
