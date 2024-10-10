package org.testcontainers.containers.schema;

import lombok.Data;
import org.testcontainers.containers.schema.annotations.May;
import org.testcontainers.containers.schema.annotations.Must;
import org.testcontainers.containers.schema.annotations.Oid;
import org.testcontainers.containers.schema.annotations.Rfc;
import org.testcontainers.containers.schema.annotations.RfcValue;

/**
 * RFC2256: a group of unique names (DN and Unique Identifier)
 */
@Data
@Rfc(RfcValue.RFC_2256)
@Oid(value = "2.5.6.17", description = "RFC2256: a group of unique names (DN and Unique Identifier")
public class GroupOfUniqueNames extends Top {

    @Must
    private String uniqueMember;

    @Must
    private String cn;

    @May
    private String businessCategory;

    @May
    private String seeAlso;

    @May
    private String owner;

    @May
    private String ou;

    @May
    private String o;

    @May
    private String description;
}
