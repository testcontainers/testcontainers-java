package org.testcontainers.containers;

import org.testcontainers.containers.JdbcDatabaseContainer;

public class VirtuosoContainer<SELF extends VirtuosoContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

	public static final String NAME = "virtuoso";
	public static final String IMAGE = "tenforce/virtuoso";
	public static final Integer JDBC_PORT = 1111;
	public static final Integer SPARQL_SERVICE_PORT = 8890;    

	public VirtuosoContainer() {
		super(IMAGE + ":latest");
	}
    
	public VirtuosoContainer(String dockerImageName) {
		super(dockerImageName);
	}
	
	@Override
	protected void configure() {
		addExposedPort(JDBC_PORT);
		addExposedPort(SPARQL_SERVICE_PORT);
		addEnv("DBA_PASSWORD", getPassword());
		addEnv("SPARQL_UPDATE", "true");
		addEnv("DEFAULT_GRAPH", "http://localhost:8890/DAV");
		addExposedPorts(JDBC_PORT, SPARQL_SERVICE_PORT);
	}

	@Override
	protected String getDriverClassName() {
		return "virtuoso.jdbc4.Driver";
	}

	@Override
	public String getJdbcUrl() {
		return "jdbc:virtuoso://" + getContainerIpAddress() + ":" + getMappedPort(JDBC_PORT);
	}
	
	public String getSparqlUrl() {
		return "http://" + getContainerIpAddress() + ":" + getMappedPort(SPARQL_SERVICE_PORT) + "/sparql";
	}

	@Override
	public String getUsername() {
		return "dba";
	}

	@Override
	public String getPassword() {
		return "myDbaPassword";
	}

	@Override
	protected String getTestQueryString() {
		return "SELECT 1";
	}

	@Override
	protected Integer getLivenessCheckPort() {
		return getMappedPort(JDBC_PORT);
	}

}
