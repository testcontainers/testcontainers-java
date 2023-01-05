package org.testcontainers.jdbc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectionUrlTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConnectionUrl1() {
        String urlString = "jdbc:tc:mysql:5.7.34://somehostname:3306/databasename?a=b&c=d";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertThat(url.getDatabaseType()).as("Database Type value is as expected").isEqualTo("mysql");
        assertThat(url.getImageTag()).as("Database Image tag value is as expected").contains("5.7.34");
        assertThat(url.getDbHostString())
            .as("Database Host String is as expected")
            .isEqualTo("somehostname:3306/databasename");
        assertThat(url.getQueryString()).as("Query String value is as expected").contains("?a=b&c=d");
        assertThat(url.getDatabaseHost()).as("Database Host value is as expected").contains("somehostname");
        assertThat(url.getDatabasePort()).as("Database Port value is as expected").contains(3306);
        assertThat(url.getDatabaseName()).as("Database Name value is as expected").contains("databasename");

        assertThat(url.getQueryParameters()).as("Parameter a is captured").containsEntry("a", "b");
        assertThat(url.getQueryParameters()).as("Parameter c is captured").containsEntry("c", "d");
    }

    @Test
    public void testConnectionUrl2() {
        String urlString = "jdbc:tc:mysql://somehostname/databasename";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertThat(url.getDatabaseType()).as("Database Type value is as expected").isEqualTo("mysql");
        assertThat(url.getImageTag()).as("Database Image tag value is as expected").isNotPresent();
        assertThat(url.getDbHostString())
            .as("Database Host String is as expected")
            .isEqualTo("somehostname/databasename");
        assertThat(url.getQueryString()).as("Query String value is as expected").isEmpty();
        assertThat(url.getDatabaseHost()).as("Database Host value is as expected").contains("somehostname");
        assertThat(url.getDatabasePort()).as("Database Port is null as expected").isNotPresent();
        assertThat(url.getDatabaseName()).as("Database Name value is as expected").contains("databasename");

        assertThat(url.getQueryParameters().isEmpty()).as("Connection Parameters set is empty").isTrue();
    }

    @Test
    public void testEmptyQueryParameter() {
        ConnectionUrl url = ConnectionUrl.newInstance("jdbc:tc:mysql://somehostname/databasename?key=");
        assertThat(url.getQueryParameters().get("key")).as("'key' property value").isEqualTo("");
    }

    @Test
    public void testTmpfsOption() {
        String urlString = "jdbc:tc:mysql://somehostname/databasename?TC_TMPFS=key:value,key1:value1";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertThat(url.getQueryParameters()).as("Connection Parameters set is empty").isEmpty();
        assertThat(url.getContainerParameters()).as("Container Parameters set is not empty").isNotEmpty();
        assertThat(url.getContainerParameters())
            .as("Container Parameter TC_TMPFS is true")
            .containsEntry("TC_TMPFS", "key:value,key1:value1");
        assertThat(url.getTmpfsOptions()).as("tmpfs option key has correct value").containsEntry("key", "value");
        assertThat(url.getTmpfsOptions()).as("tmpfs option key1 has correct value").containsEntry("key1", "value1");
    }

    @Test
    public void testCopyFilesOption() {
        String urlString =
            "jdbc:tc:mysql://somehostname/databasename" +
            "?TC_COPY_FILE=file:key1:path1" +
            "?TC_COPY_FILE=file:/key2:path2" +
            "&TC_COPY_FILE=logback-test.xml:path3:755";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertThat(url.getQueryParameters()).as("Connection Parameters set is empty").isEmpty();
        assertThat(url.getContainerParameters()).as("Container Parameters set is not empty").isNotEmpty();
        assertThat(url.getCopyFilesToContainerOptions())
            .as("copyFiles option has correct values")
            .containsOnlyKeys("path1", "path2", "path3");

        assertThat(url.getCopyFilesToContainerOptions())
            .as("copyFiles option path1 has correct value")
            .extractingByKey("path1")
            .asList()
            .hasSize(1);
        assertThat(url.getCopyFilesToContainerOptions())
            .as("copyFiles option path1 has correct value")
            .extractingByKey("path1")
            .asList()
            .element(0)
            .extracting("filesystemPath")
            .isEqualTo(Paths.get(".").toAbsolutePath().normalize() + "/key1");
        assertThat(url.getCopyFilesToContainerOptions())
            .as("copyFiles option path1 has correct value")
            .extractingByKey("path1")
            .asList()
            .element(0)
            .extracting("fileMode")
            .isEqualTo(0100000 | 0644);

        assertThat(url.getCopyFilesToContainerOptions())
            .as("copyFiles option path1 has correct value")
            .extractingByKey("path2")
            .asList()
            .hasSize(1);
        assertThat(url.getCopyFilesToContainerOptions())
            .as("copyFiles option path1 has correct value")
            .extractingByKey("path2")
            .asList()
            .element(0)
            .extracting("filesystemPath")
            .isEqualTo("/key2");
        assertThat(url.getCopyFilesToContainerOptions())
            .as("copyFiles option path1 has correct value")
            .extractingByKey("path2")
            .asList()
            .element(0)
            .extracting("fileMode")
            .isEqualTo(0100000 | 0644);

        assertThat(url.getCopyFilesToContainerOptions())
            .as("copyFiles option path1 has correct value")
            .extractingByKey("path3")
            .asList()
            .hasSize(1);
        assertThat(url.getCopyFilesToContainerOptions())
            .as("copyFiles option path1 has correct value")
            .extractingByKey("path3")
            .asList()
            .element(0)
            .extracting("filesystemPath")
            .isEqualTo(MountableFile.forClasspathResource("logback-test.xml").getFilesystemPath());
        assertThat(url.getCopyFilesToContainerOptions())
            .as("copyFiles option path1 has correct value")
            .extractingByKey("path3")
            .asList()
            .element(0)
            .extracting("fileMode")
            .isEqualTo(0100000 | 0755);
    }

    @Test
    public void testInitScriptPathCapture() {
        String urlString =
            "jdbc:tc:mysql:5.7.34://somehostname:3306/databasename?a=b&c=d&TC_INITSCRIPT=somepath/init_mysql.sql";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertThat(url.getInitScriptPath())
            .as("Database Type value is as expected")
            .contains("somepath/init_mysql.sql");
        assertThat(url.getQueryString()).as("Query String value is as expected").contains("?a=b&c=d");
        assertThat(url.getContainerParameters())
            .as("INIT SCRIPT Path exists in Container Parameters")
            .containsEntry("TC_INITSCRIPT", "somepath/init_mysql.sql");

        //Parameter sets are unmodifiable
        thrown.expect(UnsupportedOperationException.class);
        url.getContainerParameters().remove("TC_INITSCRIPT");
        url.getQueryParameters().remove("a");
    }

    @Test
    public void testInitFunctionCapture() {
        String urlString =
            "jdbc:tc:mysql:5.7.34://somehostname:3306/databasename?a=b&c=d&TC_INITFUNCTION=org.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertThat(url.getInitFunction()).as("Init Function parameter exists").isPresent();

        assertThat(url.getInitFunction().get().getClassName())
            .as("Init function class is as expected")
            .isEqualTo("org.testcontainers.jdbc.JDBCDriverTest");
        assertThat(url.getInitFunction().get().getMethodName())
            .as("Init function class is as expected")
            .isEqualTo("sampleInitFunction");
    }

    @Test
    public void testDaemonCapture() {
        String urlString = "jdbc:tc:mysql:5.7.34://somehostname:3306/databasename?a=b&c=d&TC_DAEMON=true";
        ConnectionUrl url = ConnectionUrl.newInstance(urlString);

        assertThat(url.isInDaemonMode()).as("Daemon flag is set to true.").isTrue();
    }
}
