package org.testcontainers.containers;

import com.github.dockerjava.api.model.Ulimit;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import static java.time.temporal.ChronoUnit.SECONDS;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.LicenseAcceptance;
import org.testcontainers.utility.DockerImageName;


public class HANAContainer<SELF extends HANAContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

	// Image defaults
    public static final String NAME = "hana";
    public static final String IMAGE = "store/saplabs/hanaexpress";
    public static final String DEFAULT_TAG = "2.00.045.00.20200121.1";
    
    public static final String DB_DRIVER = "com.sap.db.jdbc.Driver";
    
    // Ports required for jdbc connection to system / tenant db.
    public static final Integer SYSTEM_PORT = 39017;
    public static final Integer TENANT_PORT = 39041;

    // DB information
    private static final String systemDBName = "SYSTEMDB";
    private static final String tenantDBName = "HXE";    
    private static final String username = "SYSTEM";
    
    public static final long uLimitsSoft = 1048576;
    public static final long uLimitsHard = 1048576;
    
    private String password = "HXEHana1";
    
    // Password policy default: Capital letter, lower case letter, number and length >= 8
    private static final Pattern[] PASSWORD_CATEGORY_VALIDATION_PATTERNS = new Pattern[]{
            Pattern.compile("[A-Z]+"),
            Pattern.compile("[a-z]+"),
            Pattern.compile("[0-9]+")
        };

    public HANAContainer(DockerImageName image) {
    	
        super(image);
        
        // create bindings -- using random ports on host machine. Otherwise we can crash if the ports are used: https://github.com/testcontainers/testcontainers-java/issues/256
        addExposedPorts(39013, 39017, 39041, 39042, 39043, 39044, 39045, 1128, 1129, 59013, 59014);
        
        // create ulimits
        Ulimit[] ulimits = new Ulimit[] { new Ulimit("nofile", uLimitsSoft, uLimitsHard) };
        
        // create sysctls Map.
        Map<String, String> sysctls = new HashMap<String, String>();

        sysctls.put("kernel.shmmax", "1073741824");
        sysctls.put("net.ipv4.ip_local_port_range", "40000 60999");
        sysctls.put("kernel.shmmni", "524288");
        sysctls.put("kernel.shmall", "8388608");
        
        // Apply mounts, ulimits and sysctls.    
        this.withCreateContainerCmdModifier(it -> it.getHostConfig()
        		.withUlimits(ulimits)
        		.withSysctls(sysctls)
        );

        // Arguments for Image.
        this.withCommand(
        		"--master-password " + password + 
        		" --agree-to-sap-license");
        
        // Determine if container is ready.
        this.waitStrategy = new LogMessageWaitStrategy()
	        .withRegEx(".*Startup finished!*\\s")
	        .withTimes(1)
	        .withStartupTimeout(Duration.of(600, SECONDS));
    }

    @Override
    protected void configure() {
    	/*
    	 * Enforce that the license is accepted - do not remove.
    	 * 
    	 * License available at: https://www.sap.com/docs/download/cmp/2016/06/sap-hana-express-dev-agmt-and-exhibit.pdf
    	 * 
    	 */
    	
        // If license was not accepted programmatically, check if it was accepted via resource file
        if (!getEnvMap().containsKey("AGREE_TO_SAP_LICENSE")) {
            LicenseAcceptance.assertLicenseAccepted(this.getDockerImageName());
            acceptLicense();
        }
    }

    /**
     * Set a custom password. Password policy default: Capital letter, lower case letter, number and length over 7
     * 
     * @param password the password you want to use for accessing the database.
     */
    public SELF withPassword(String password) {
    	checkPasswordStrength(password);
    	this.password = password;
		return self();
    }
    
    /**
     * Accepts the license for the SAP HANA Express container by setting the ACCEPT_EULA=Y
     * Calling this method will automatically accept the license at: https://www.sap.com/docs/download/cmp/2016/06/sap-hana-express-dev-agmt-and-exhibit.pdf
     * 
     * @return The container itself with an environment variable accepting the SAP HANA Express license
     */
    public SELF acceptLicense() {
        addEnv("AGREE_TO_SAP_LICENSE", "Y");
        return self();
    }

    @NotNull
    @Override
    protected Set<Integer> getLivenessCheckPorts() {
        return new HashSet<>(Arrays.asList(new Integer[] {getMappedPort(TENANT_PORT), getMappedPort(SYSTEM_PORT)}));
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }

    @Override
    public String getDriverClassName() {
    	return DB_DRIVER;
    }

    /**
     * Default database name getter. Because of HANAs two databases it will return the tenant database. If you want to get the name of the system database, use 'getSystemDatabaseName'.
     * 
     * @return The name of the tenant database.
     */
    @Override
    public String getDatabaseName() {
        return getTenantDatabaseName();
    }
    
    /**
     * Get the name of the HANA system database.
     * 
     * @return The name of the HANA system database.
     */
    public String getSystemDatabaseName() {
    	return systemDBName;
    }

    /**
     * Get the name of the HANA tenant database.
     * 
     * @return The name of the HANA tenant database.
     */
    public String getTenantDatabaseName() {
    	return tenantDBName;
    }
    
    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getTestQueryString() {
        return "SELECT 1 FROM sys.dummy";
    }

    /**
     * Get the port for the system database and general connections.
     * 
     * @return port of the system database.
     */
    @NotNull
    public Integer getSystemPort() {
        return getMappedPort(SYSTEM_PORT);
    }
    
    /**
     * Get the port for the tenant database.
     * 
     * @return port of the tenant database.
     */
    @NotNull
    public Integer getTenantPort() {
        return getMappedPort(TENANT_PORT);
    }
    
    /**
     * Get the Jdbc url used to connect to the databases.
     * 
     * @return jdbc url
     */
    @Override
    public String getJdbcUrl() {
    	return "jdbc:sap://" + getContainerIpAddress() + ":" + getSystemPort() + "/";
    }
    
    /**
     * Modification of the default connection string because of HANA specific database selection.
     * 
     * Query will connect to the tenant database per default. If you want to connect to the system database, supply '?databaseName=SYSTEMDB' as queryString
     * 
     * @param queryString your custom query attached to the connection string.
     * @return Connection object.
     * 
     */
    @Override
    public Connection createConnection(String queryString) throws SQLException, NoDriverFoundException {	
    	
    	if(queryString == null || queryString.trim().isEmpty()) {
    		queryString = "?databaseName=" + tenantDBName;
    	} else {
        	// Remove ? from queryString if supplied.
        	if(Character.compare(queryString.charAt(0), '?') == 0) {
        		queryString = queryString.substring(1);
        	}
        	queryString = "?databaseName=" + tenantDBName + "&" + queryString;
    	} 	
    	
		return super.createConnection(queryString);
    }
    
    /**
     * Private function to check if a user supplied password matches the security requirements of SAP HANA
     * 
     * @param password which needs to be checked for security requirements
     */
    private void checkPasswordStrength(String password) {

        if (password == null) {
            throw new IllegalArgumentException("Null password is not allowed");
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException("Password should be at least 8 characters long");
        }

        if (password.length() > 128) {
            throw new IllegalArgumentException("Password can be up to 128 characters long");
        }

        long satisfiedCategories = Stream.of(PASSWORD_CATEGORY_VALIDATION_PATTERNS)
            .filter(p -> p.matcher(password).find())
            .count();

        if (satisfiedCategories < 3) {
            throw new IllegalArgumentException(
                "Password must contain characters from the following three categories:\n" +
                    " - Latin uppercase letters (A through Z)\n" +
                    " - Latin lowercase letters (a through z)\n" +
                    " - Base 10 digits (0 through 9).\n"
            );
        }
    }
}