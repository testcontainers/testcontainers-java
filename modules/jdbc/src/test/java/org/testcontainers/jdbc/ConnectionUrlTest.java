package org.testcontainers.jdbc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Optional;

import static org.rnorth.visibleassertions.VisibleAssertions.*;

public class ConnectionUrlTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void testConnectionUrl1() {
        String urlString = "jdbc:tc:mysql:5.7.34://somehostname:3306/databasename?a=b&c=d";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertEquals("Database Type value is as expected", "mysql", url.getDatabaseType());
        assertEquals("Database Image tag value is as expected", "5.7.34", url.getImageTag().get());
        assertEquals("Database Host String is as expected", "somehostname:3306/databasename", url.getDbHostString());
        assertEquals("Query String value is as expected", "?a=b&c=d", url.getQueryString().get());
        assertEquals("Database Host value is as expected", "somehostname", url.getDatabaseHost().get());
        assertEquals("Database Port value is as expected", 3306, url.getDatabasePort().get());
        assertEquals("Database Name value is as expected", "databasename", url.getDatabaseName().get());

        assertEquals("Parameter a is captured", "b", url.getQueryParameters().get("a"));
        assertEquals("Parameter c is captured", "d", url.getQueryParameters().get("c"));
    }


    @Test
    public void testConnectionUrl2() {
        String urlString = "jdbc:tc:mysql://somehostname/databasename";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertEquals("Database Type value is as expected", "mysql", url.getDatabaseType());
        assertFalse("Database Image tag value is as expected", url.getImageTag().isPresent());
        assertEquals("Database Host String is as expected", "somehostname/databasename", url.getDbHostString());
        assertEquals("Query String value is as expected", Optional.empty(), url.getQueryString());
        assertEquals("Database Host value is as expected", "somehostname", url.getDatabaseHost().get());
        assertFalse("Database Port is null as expected", url.getDatabasePort().isPresent());
        assertEquals("Database Name value is as expected", "databasename", url.getDatabaseName().get());

        assertTrue("Connection Parameters set is empty", url.getQueryParameters().isEmpty());

    }

    @Test
    public void testEmptyQueryParameter() {
        ConnectionUrl url = ConnectionUrl.newInstance("jdbc:tc:mysql://somehostname/databasename?key=");
        assertEquals("'key' property value", "", url.getQueryParameters().get("key"));
    }

    @Test
    public void testTmpfsOption() {
        String urlString = "jdbc:tc:mysql://somehostname/databasename?TC_TMPFS=key:value,key1:value1";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertTrue("Connection Parameters set is empty", url.getQueryParameters().isEmpty());
        assertFalse("Container Parameters set is not empty", url.getContainerParameters().isEmpty());
        assertEquals("Container Parameter TC_TMPFS is true", "key:value,key1:value1", url.getContainerParameters().get("TC_TMPFS"));
        assertTrue("tmpfs option key exists", url.getTmpfsOptions().containsKey("key"));
        assertEquals("tmpfs option key has correct value", "value" , url.getTmpfsOptions().get("key"));
        assertTrue("tmpfs option key1 exists", url.getTmpfsOptions().containsKey("key1"));
        assertEquals("tmpfs option key1 has correct value", "value1" , url.getTmpfsOptions().get("key1"));
    }


    @Test
    public void testInitScriptPathCapture() {
        String urlString = "jdbc:tc:mysql:5.7.34://somehostname:3306/databasename?a=b&c=d&TC_INITSCRIPT=somepath/init_mysql.sql";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertEquals("Database Type value is as expected", "somepath/init_mysql.sql", url.getInitScriptPath().get());
        assertEquals("Query String value is as expected", "?a=b&c=d", url.getQueryString().get());
        assertEquals("INIT SCRIPT Path exists in Container Parameters", "somepath/init_mysql.sql", url.getContainerParameters().get("TC_INITSCRIPT"));

        //Parameter sets are unmodifiable
        thrown.expect(UnsupportedOperationException.class);
        url.getContainerParameters().remove("TC_INITSCRIPT");
        url.getQueryParameters().remove("a");

    }

    @Test
    public void testInitFunctionCapture() {
        String urlString = "jdbc:tc:mysql:5.7.34://somehostname:3306/databasename?a=b&c=d&TC_INITFUNCTION=org.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertTrue("Init Function parameter exists", url.getInitFunction().isPresent());

        assertEquals("Init function class is as expected", "org.testcontainers.jdbc.JDBCDriverTest", url.getInitFunction().get().getClassName());
        assertEquals("Init function class is as expected", "sampleInitFunction", url.getInitFunction().get().getMethodName());

    }

    @Test
    public void testDaemonCapture() {
        String urlString = "jdbc:tc:mysql:5.7.34://somehostname:3306/databasename?a=b&c=d&TC_DAEMON=true";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertTrue("Daemon flag is set to true.", url.isInDaemonMode());

    }
}
