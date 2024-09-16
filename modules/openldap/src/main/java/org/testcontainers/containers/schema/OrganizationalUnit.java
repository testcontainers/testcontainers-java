package org.testcontainers.containers.schema;

import lombok.Data;
import org.testcontainers.containers.schema.annotations.May;
import org.testcontainers.containers.schema.annotations.Must;
import org.testcontainers.containers.schema.annotations.Oid;
import org.testcontainers.containers.schema.annotations.Rfc;
import org.testcontainers.containers.schema.annotations.RfcValue;

/**
 * RFC22256: an organization unit
 */
@Data
@Rfc(RfcValue.RFC_2256)
@Oid(value = "2.5.6.5", description = "RFC2256: an organizational unit")
public class OrganizationalUnit extends Top {

    @Must
    private String ou;

    @May
    private String userPassword;
    @May
    private String searchGuide;
    @May
    private String seeAlso;
    @May
    private String businessCategory;
    @May
    private String x121Address;
    @May
    private String registeredAddress;
    @May
    private String destinationIndicator;
    @May
    private String preferredDeliveryMethod;
    @May
    private String telexNumber;
    @May
    private String teletexTerminalIdentifier;
    @May
    private String telephoneNumber;
    @May
    private String internationaliSDNNumber;
    @May
    private String facsimileTelephoneNumber;
    @May
    private String street;
    @May
    private String postOfficeBox;
    @May
    private String postalCode;
    @May
    private String postalAddress;
    @May
    private String physicalDeliveryOfficeName;
    @May
    private String st;
    @May
    private String l;
    @May
    private String description;
}
