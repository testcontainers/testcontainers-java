package org.testcontainers.jdbc;


import static org.rnorth.visibleassertions.VisibleAssertions.*;

import org.junit.Test;

public class ConnectionUrlTest {

  @Test
  public void testConnectionUrl1() {
    String urlString = "jdbc:tc:mysql:5.6.23://somehostname:3306/databasename?a=b&c=d";
    ConnectionUrl url = new ConnectionUrl(urlString);
    url.parseUrl();
    
    assertEquals("Database Type value is as expected", "mysql", url.getDatabaseType());
    assertEquals("Database Image tag value is as expected", "5.6.23", url.getImageTag());
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
    ConnectionUrl url = new ConnectionUrl(urlString);
    url.parseUrl();
    
    assertEquals("Database Type value is as expected", "mysql", url.getDatabaseType());
    assertEquals("Database Image tag value is as expected", "latest", url.getImageTag());
    assertEquals("Database Host String is as expected", "somehostname/databasename", url.getDbHostString());
    assertEquals("Query String value is as expected", "?", url.getQueryString().get());
    assertEquals("Database Host value is as expected", "somehostname", url.getDatabaseHost().get());
    assertFalse("Database Port is null as expected", url.getDatabasePort().isPresent());
    assertEquals("Database Name value is as expected", "databasename", url.getDatabaseName().get());
    
    assertTrue("Connection Parameters set is empty", url.getQueryParameters().isEmpty());
  }
  
  @Test
  public void testInitScriptPathCapture() {
    String urlString = "jdbc:tc:mysql:5.6.23://somehostname:3306/databasename?a=b&c=d&TC_INITSCRIPT=somepath/init_mysql.sql";
    ConnectionUrl url = new ConnectionUrl(urlString);
    url.parseUrl();
    
    assertEquals("Database Type value is as expected", "somepath/init_mysql.sql", url.getInitScriptPath().get());
    assertEquals("Query String value is as expected", "?a=b&c=d", url.getQueryString().get());
    assertEquals("INIT SCRIPT Path exists in Container Parameters", "somepath/init_mysql.sql", url.getContainerParameters().get("TC_INITSCRIPT"));
    
  }
  
  @Test
  public void testInitFunctionCapture() {
    String urlString = "jdbc:tc:mysql:5.6.23://somehostname:3306/databasename?a=b&c=d&TC_INITFUNCTION=org.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction";
    ConnectionUrl url = new ConnectionUrl(urlString);
    url.parseUrl();
    
    assertTrue("Init Function parameter exists", url.getInitFunction().isPresent());
    
    assertEquals("Init function class is as expected", "org.testcontainers.jdbc.JDBCDriverTest", url.getInitFunction().get().getClassName());
    assertEquals("Init function class is as expected", "sampleInitFunction", url.getInitFunction().get().getMethodName());
    
  }
  
  @Test
  public void testDaemonCapture() {
    String urlString = "jdbc:tc:mysql:5.6.23://somehostname:3306/databasename?a=b&c=d&TC_DAEMON=true";
    ConnectionUrl url = new ConnectionUrl(urlString);
    url.parseUrl();
    
    assertTrue("Daemon flag is set to true.",url.isInDaemonMode());
    
  }
  
  
}
