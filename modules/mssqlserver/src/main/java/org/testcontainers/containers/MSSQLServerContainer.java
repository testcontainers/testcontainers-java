package org.testcontainers.containers;

import org.testcontainers.utility.LicenseAcceptance;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author Stefan Hufschmidt
 */
public class MSSQLServerContainer<SELF extends MSSQLServerContainer<SELF>> extends JdbcDatabaseContainer<SELF> {
    public static final String NAME = "mssqlserver";
    public static final String IMAGE = "microsoft/mssql-server-linux";
    public static final String DEFAULT_TAG = "2017-CU6";

    public static final Integer MS_SQL_SERVER_PORT = 1433;
    private String username = "SA";
    private String password = "A_Str0ng_Required_Password";

    private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 240;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 240;

    private static final Pattern[] PASSWORD_CATEGORY_VALIDATION_PATTERNS = new Pattern[] {
        Pattern.compile("[A-Z]+"),
        Pattern.compile("[a-z]+"),
        Pattern.compile("[0-9]+"),
        Pattern.compile("[^a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE)
    };

    public MSSQLServerContainer() {
        this(IMAGE + ":" + DEFAULT_TAG);
    }

    public MSSQLServerContainer(final String dockerImageName) {
        super(dockerImageName);
        withStartupTimeoutSeconds(DEFAULT_STARTUP_TIMEOUT_SECONDS);
        withConnectTimeoutSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS);
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(MS_SQL_SERVER_PORT);
    }

    @Override
    protected void configure() {
        addExposedPort(MS_SQL_SERVER_PORT);

        LicenseAcceptance.assertLicenseAccepted(this.getDockerImageName());
        addEnv("ACCEPT_EULA", "Y");

        addEnv("SA_PASSWORD", password);
    }

    @Override
    public String getDriverClassName() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:sqlserver://" + getContainerIpAddress() + ":" + getMappedPort(MS_SQL_SERVER_PORT);
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
        return "SELECT 1";
    }

    @Override
    public SELF withPassword(final String password) {
        checkPasswordStrength(password);
        this.password = password;
        return self();
    }

    private void checkPasswordStrength(String password){

        if(password == null){
            throw new IllegalArgumentException("Null password is not allowed");
        }

        if(password.length() < 8){
            throw new IllegalArgumentException("Password should be at least 8 characters long");
        }

        if(password.length() > 128){
            throw new IllegalArgumentException("Password can be up to 128 characters long");
        }

        long satisfiedCategories = Stream.of(PASSWORD_CATEGORY_VALIDATION_PATTERNS)
            .filter(p -> p.matcher(password).find())
            .count();

        if(satisfiedCategories < 3){
            throw new IllegalArgumentException(
                "Password must contains characters from three of the following four categories:\n" +
                " - Latin uppercase letters (A through Z)\n" +
                " - Latin lowercase letters (a through z)\n" +
                " - Base 10 digits (0 through 9)\n" +
                " - Non-alphanumeric characters such as: exclamation point (!), dollar sign ($), number sign (#), " +
                    "or percent (%)."
            );
        }

    }
}
