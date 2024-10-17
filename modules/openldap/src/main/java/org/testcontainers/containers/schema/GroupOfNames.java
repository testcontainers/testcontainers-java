package org.testcontainers.containers.schema;

import lombok.Data;
import org.testcontainers.containers.schema.annotations.May;
import org.testcontainers.containers.schema.annotations.Must;
import org.testcontainers.containers.schema.annotations.Oid;
import org.testcontainers.containers.schema.annotations.Rfc;
import org.testcontainers.containers.schema.annotations.RfcValue;

/**
 * RFC2256: a group of names (DNs)
 */
@Data
@Rfc(RfcValue.RFC_2256)
@Oid(value = "2.5.6.9", description = "RFC2256: a group of names (DNs)")
public class GroupOfNames extends Top {

    @Must
    private String member;

    @Must
    private String cn;

    @May
    public String businessCategory;

    @May
    public String seeAlso;

    @May
    public String owner;

    @May
    public String ou;

    @May
    public String o;

    @May
    public String description;
}
