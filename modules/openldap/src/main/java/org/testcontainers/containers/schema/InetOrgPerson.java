package org.testcontainers.containers.schema;


import lombok.Data;
import org.testcontainers.containers.schema.annotations.May;
import org.testcontainers.containers.schema.annotations.Oid;
import org.testcontainers.containers.schema.annotations.Rfc;
import org.testcontainers.containers.schema.annotations.RfcValue;

/**
 * RFC2798: Internet Organizational Person
 */
@Data
@Rfc(RfcValue.RFC_2798)
@Oid(value = "2.16.840.1.113730.3.2.2", description = "RFC2798: Internet Organizational Person")
public class InetOrgPerson extends OrganizationalPerson implements PkiUser {

    @May
    private String audio;
    @May
    private String businessCategory;
    @May
    private String carLicense;
    @May
    private String departmentNumber;
    @May
    private String displayName;
    @May
    private String employeeNumber;
    @May
    private String employeeType;
    @May
    private String givenName;
    @May
    private String homePhone;
    @May
    private String homePostalAddress;
    @May
    private String initials;
    @May
    private String jpegPhoto;
    @May
    private String labeledURI;
    @May
    private String mail;
    @May
    private String manager;
    @May
    private String mobile;
    @May
    private String o;
    @May
    private String pager;
    @May
    private String photo;
    @May
    private String roomNumber;
    @May
    private String secretary;
    @May
    private String uid;
    @May
    private String userCertificate;
    @May
    private String x500uniqueIdentifier;
    @May
    private String preferredLanguage;
    @May
    private String userSMIMECertificate;
    @May
    private String userPKCS12;
}
