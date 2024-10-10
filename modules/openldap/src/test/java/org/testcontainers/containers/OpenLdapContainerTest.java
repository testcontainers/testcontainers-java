package org.testcontainers.containers;

import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import java.net.MalformedURLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test shows the pattern to use the OpenLdapContainer @ClassRule for a junit test.
 */
public class OpenLdapContainerTest {

    private static final Logger LOG = LoggerFactory.getLogger(OpenLdapContainerTest.class);

    @ClassRule
    public static OpenLdapContainer ldapContainer = new OpenLdapContainer();

    /**
     * Check anonymous access
     *
     * @throws NamingException
     * @throws MalformedURLException
     */
    @Test
    public void testAnonymousAccess() throws NamingException, MalformedURLException {
        DirContext context = null;
        try {
            context = ldapContainer.connectAnonymously();
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }

    /**
     * Check admin access
     *
     * @throws NamingException
     * @throws MalformedURLException
     */
    @Test
    public void testAdminAccess() throws NamingException, MalformedURLException {
        DirContext context = null;
        try {
            context = ldapContainer.connectAsAdmin();
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }

    /**
     * Check whether listObjectClasses returns anything
     *
     * @throws NamingException
     * @throws MalformedURLException
     */
    @Test
    public void testListObjectClasses() throws NamingException, MalformedURLException {
        DirContext context = null;
        try {
            context = ldapContainer.connectAsAdmin();

            final List<ObjectClassInformation> objectClasses = ldapContainer.listObjectClasses();
            assertThat(objectClasses.isEmpty()).isFalse();
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }
}
