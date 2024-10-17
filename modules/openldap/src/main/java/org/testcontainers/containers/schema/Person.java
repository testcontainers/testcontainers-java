package org.testcontainers.containers.schema;

import lombok.Data;
import org.testcontainers.containers.schema.annotations.May;
import org.testcontainers.containers.schema.annotations.Must;
import org.testcontainers.containers.schema.annotations.Oid;
import org.testcontainers.containers.schema.annotations.Rfc;
import org.testcontainers.containers.schema.annotations.RfcValue;

/**
 * RFC2256: A Person
 */
@Data
@Rfc(RfcValue.RFC_2256)
@Oid(value = "2.5.6.6", description = "RFC2256: a person")
public class Person extends Top {

    @Must
    private String sn;

    @Must
    private String cn;

    @May
    private String userPassword;

    @May
    private String telephoneNumber;

    @May
    private String seeAlso;

    @May
    private String description;
}
