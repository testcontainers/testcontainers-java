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
        String urlString = "jdbc:tc:mysql:5.6.23://somehostname:3306/databasename?a=b&c=d";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertEquals("Database Type value is as expected", "mysql", url.getDatabaseType());
        assertEquals("Database image name value is as expected", "mysql", url.getImageName());
        assertEquals("Database Image tag value is as expected", "5.6.23", url.getImageTag().get());
        assertFalse("Database registry host is as expected", url.getRegistry().isPresent());
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
        assertEquals("Database image name value is as expected", "mysql", url.getImageName());
        assertFalse("Database Image tag value is as expected", url.getImageTag().isPresent());
        assertFalse("Database registry host is as expected", url.getRegistry().isPresent());
        assertEquals("Database registry host is as expected", Optional.empty(), url.getRegistry());
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
        String urlString = "jdbc:tc:mysql:5.6.23://somehostname:3306/databasename?a=b&c=d&TC_INITSCRIPT=somepath/init_mysql.sql";
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
        String urlString = "jdbc:tc:mysql:5.6.23://somehostname:3306/databasename?a=b&c=d&TC_INITFUNCTION=org.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertTrue("Init Function parameter exists", url.getInitFunction().isPresent());

        assertEquals("Init function class is as expected", "org.testcontainers.jdbc.JDBCDriverTest", url.getInitFunction().get().getClassName());
        assertEquals("Init function class is as expected", "sampleInitFunction", url.getInitFunction().get().getMethodName());

    }

    @Test
    public void testDaemonCapture() {
        String urlString = "jdbc:tc:mysql:5.6.23://somehostname:3306/databasename?a=b&c=d&TC_DAEMON=true";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertTrue("Daemon flag is set to true.", url.isInDaemonMode());

    }

    @Test
    public void testAliasRequiredParam() {
        assertThrows("Alias name is required",
            NullPointerException.class,
            () -> ImageAlias.addAlias(null, null));
        assertThrows("Alias name is required",
            NullPointerException.class,
            () -> ImageAlias.addAlias(null, null, null, null));
        assertThrows("Alias name is required",
            NullPointerException.class,
            () -> ImageAlias.addAlias(null, null, null, null, null));

        assertThrows("Image type is required",
            NullPointerException.class,
            () -> ImageAlias.addAlias("alias", null));
        assertThrows("Image type is required",
            NullPointerException.class,
            () -> ImageAlias.addAlias("alias", null, null, null));
        assertThrows("Image type is required",
            NullPointerException.class,
            () -> ImageAlias.addAlias("alias", null, null, null, null));
    }

    @Test
    public void testAlias() {
        ImageAlias.addAlias("mypostgres", "postgres");

        String urlString = "jdbc:tc:mypostgres://somehostname:3306/databasename?a=b&c=d";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertEquals("Database Type value is as expected", "postgres", url.getDatabaseType());
        assertEquals("Database image name value is as expected", "postgres", url.getImageName());
        assertFalse("Database Image tag value is as expected", url.getImageTag().isPresent());
        assertEquals("Database Host String is as expected", "somehostname:3306/databasename", url.getDbHostString());
        assertFalse("Database registry host is as expected", url.getRegistry().isPresent());
        assertEquals("Query String value is as expected", "?a=b&c=d", url.getQueryString().get());
        assertEquals("Database Host value is as expected", "somehostname", url.getDatabaseHost().get());
        assertEquals("Database Port value is as expected", 3306, url.getDatabasePort().get());
        assertEquals("Database Name value is as expected", "databasename", url.getDatabaseName().get());

        assertEquals("Parameter a is captured", "b", url.getQueryParameters().get("a"));
        assertEquals("Parameter c is captured", "d", url.getQueryParameters().get("c"));
    }

    @Test
    public void testAliasWithCustomImageName() {
        ImageAlias.addAlias("mypostgres", "postgres", "my_postgres_image", null);

        String urlString = "jdbc:tc:mypostgres://somehostname:3306/databasename?a=b&c=d";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertEquals("Database Type value is as expected", "postgres", url.getDatabaseType());
        assertEquals("Database image name value is as expected", "my_postgres_image", url.getImageName());
        assertFalse("Database Image tag value is as expected", url.getImageTag().isPresent());
        assertEquals("Database Host String is as expected", "somehostname:3306/databasename", url.getDbHostString());
        assertFalse("Database registry host is as expected", url.getRegistry().isPresent());
        assertEquals("Query String value is as expected", "?a=b&c=d", url.getQueryString().get());
        assertEquals("Database Host value is as expected", "somehostname", url.getDatabaseHost().get());
        assertEquals("Database Port value is as expected", 3306, url.getDatabasePort().get());
        assertEquals("Database Name value is as expected", "databasename", url.getDatabaseName().get());

        assertEquals("Parameter a is captured", "b", url.getQueryParameters().get("a"));
        assertEquals("Parameter c is captured", "d", url.getQueryParameters().get("c"));
    }

    @Test
    public void testAliasWithCustomImageNameAndCustomImageTag() {
        ImageAlias.addAlias("mypostgres", "postgres", "my_postgres_image", "some_tag");

        String urlString = "jdbc:tc:mypostgres://somehostname:3306/databasename?a=b&c=d";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertEquals("Database Type value is as expected", "postgres", url.getDatabaseType());
        assertEquals("Database image name value is as expected", "my_postgres_image", url.getImageName());
        assertEquals("Database Image tag value is as expected", "some_tag", url.getImageTag().get());
        assertEquals("Database Host String is as expected", "somehostname:3306/databasename", url.getDbHostString());
        assertFalse("Database registry host is as expected", url.getRegistry().isPresent());
        assertEquals("Query String value is as expected", "?a=b&c=d", url.getQueryString().get());
        assertEquals("Database Host value is as expected", "somehostname", url.getDatabaseHost().get());
        assertEquals("Database Port value is as expected", 3306, url.getDatabasePort().get());
        assertEquals("Database Name value is as expected", "databasename", url.getDatabaseName().get());

        assertEquals("Parameter a is captured", "b", url.getQueryParameters().get("a"));
        assertEquals("Parameter c is captured", "d", url.getQueryParameters().get("c"));
    }

    @Test
    public void testAliasWithCustomImageTag() {
        ImageAlias.addAlias("mypostgres", "postgres", null, "some_tag");

        String urlString = "jdbc:tc:mypostgres://somehostname:3306/databasename?a=b&c=d";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertEquals("Database Type value is as expected", "postgres", url.getDatabaseType());
        assertEquals("Database image name value is as expected", "postgres", url.getImageName());
        assertEquals("Database Image tag value is as expected", "some_tag", url.getImageTag().get());
        assertEquals("Database Host String is as expected", "somehostname:3306/databasename", url.getDbHostString());
        assertFalse("Database registry host is as expected", url.getRegistry().isPresent());
        assertEquals("Query String value is as expected", "?a=b&c=d", url.getQueryString().get());
        assertEquals("Database Host value is as expected", "somehostname", url.getDatabaseHost().get());
        assertEquals("Database Port value is as expected", 3306, url.getDatabasePort().get());
        assertEquals("Database Name value is as expected", "databasename", url.getDatabaseName().get());

        assertEquals("Parameter a is captured", "b", url.getQueryParameters().get("a"));
        assertEquals("Parameter c is captured", "d", url.getQueryParameters().get("c"));
    }

    @Test
    public void testAliasWithCustomImageNameAndCustomImageTagAndCustomHost() {
        ImageAlias.addAlias("mypostgres", "postgres", null, null, "com.custom.registry");

        String urlString = "jdbc:tc:mypostgres://somehostname:3306/databasename?a=b&c=d";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertEquals("Database Type value is as expected", "postgres", url.getDatabaseType());
        assertEquals("Database image name value is as expected", "postgres", url.getImageName());
        assertFalse("Database Image tag value is as expected", url.getImageTag().isPresent());
        assertEquals("Database Host String is as expected", "somehostname:3306/databasename", url.getDbHostString());
        assertEquals("Database registry host is as expected", "com.custom.registry", url.getRegistry().get());
        assertEquals("Query String value is as expected", "?a=b&c=d", url.getQueryString().get());
        assertEquals("Database Host value is as expected", "somehostname", url.getDatabaseHost().get());
        assertEquals("Database Port value is as expected", 3306, url.getDatabasePort().get());
        assertEquals("Database Name value is as expected", "databasename", url.getDatabaseName().get());

        assertEquals("Parameter a is captured", "b", url.getQueryParameters().get("a"));
        assertEquals("Parameter c is captured", "d", url.getQueryParameters().get("c"));
    }
}
