package org.testcontainers.containers;

import lombok.Data;

/**
 * LDAP configuration
 */
@Data
public class OpenLdapConfiguration {

    private Boolean allowEmptyPassword;

    private Boolean containerDebuggingEnabled;

    private Boolean allowAnonBinding;

    private String baseDN;

    private String adminUsername;

    private String adminPassword;

    private Boolean configAdminEnabled;

    private String configAdminUsername;

    private String configAdminPassword;

    private String userDC;

    private String group;

    private Boolean enableTls;

    private Boolean requireTls;
}
