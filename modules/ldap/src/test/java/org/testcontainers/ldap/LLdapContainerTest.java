package org.testcontainers.ldap;

import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPURL;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LLdapContainerTest {

    @Test
    public void test() throws LDAPException {
        try ( // container {
            LLdapContainer lldap = new LLdapContainer("lldap/lldap:v0.6.1-alpine")
            // }
        ) {
            lldap.start();
            LDAPConnection connection = new LDAPConnection(lldap.getHost(), lldap.getLdapPort());
            BindResult result = connection.bind(lldap.getUser(), lldap.getPassword());
            assertThat(result).isNotNull();
        }
    }

    @Test
    public void testUsingLdapUrl() throws LDAPException {
        try (LLdapContainer lldap = new LLdapContainer("lldap/lldap:v0.6.1-alpine")) {
            lldap.start();

            LDAPURL ldapUrl = new LDAPURL(lldap.getLdapUrl());
            LDAPConnection connection = new LDAPConnection(ldapUrl.getHost(), ldapUrl.getPort());
            BindResult result = connection.bind(lldap.getUser(), lldap.getPassword());
            assertThat(result).isNotNull();
        }
    }

    @Test
    public void testWithCustomBaseDn() throws LDAPException {
        try (
            LLdapContainer lldap = new LLdapContainer("lldap/lldap:v0.6.1-alpine")
                .withBaseDn("dc=testcontainers,dc=org")
        ) {
            lldap.start();

            assertThat(lldap.getBaseDn()).isEqualTo("dc=testcontainers,dc=org");

            LDAPURL ldapUrl = new LDAPURL(lldap.getLdapUrl());
            LDAPConnection connection = new LDAPConnection(ldapUrl.getHost(), ldapUrl.getPort());
            BindResult result = connection.bind(lldap.getUser(), lldap.getPassword());
            assertThat(result).isNotNull();
        }
    }

    @Test
    public void testWithCustomUserPass() throws LDAPException {
        try (LLdapContainer lldap = new LLdapContainer("lldap/lldap:v0.6.1-alpine").withUserPass("adminPas$word")) {
            lldap.start();

            LDAPURL ldapUrl = new LDAPURL(lldap.getLdapUrl());
            LDAPConnection connection = new LDAPConnection(ldapUrl.getHost(), ldapUrl.getPort());
            BindResult result = connection.bind(lldap.getUser(), lldap.getPassword());
            assertThat(result).isNotNull();
        }
    }
}
