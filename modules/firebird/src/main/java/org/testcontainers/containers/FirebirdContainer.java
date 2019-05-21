package org.testcontainers.containers;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;

@Slf4j
public class FirebirdContainer<SELF extends FirebirdContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

    public static final String NAME = "firebird";
    public static final String ALTERNATE_NAME = "firebirdsql";
    public static final String IMAGE = "jacobalberty/firebird";
    public static final String DEFAULT_TAG = "3.0.4";

    public static final Integer FIREBIRD_PORT = 3050;
    private static final String FIREBIRD_SYSDBA = "sysdba";
    private static final int ARC4_REQUIRED_BITS = 160;

    private String databaseName = "test";
    private String username = "test";
    private String password = "test";
    private boolean enableLegacyClientAuth;
    private String timeZone = ZoneId.systemDefault().getId();
    private boolean enableWireCrypt;
    private String sysdbaPassword;

    public FirebirdContainer() {
        this(IMAGE + ":" + DEFAULT_TAG);
    }

    public FirebirdContainer(String dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected void configure() {
        addExposedPort(FIREBIRD_PORT);
        addEnv("TZ", timeZone);
        addEnv("FIREBIRD_DATABASE", databaseName);

        if (FIREBIRD_SYSDBA.equalsIgnoreCase(username)) {
            addEnv("ISC_PASSWORD", password);
        } else {
            addEnv("FIREBIRD_USER", username);
            addEnv("FIREBIRD_PASSWORD", password);
            if (sysdbaPassword != null) {
                addEnv("ISC_PASSWORD", sysdbaPassword);
            }
        }

        if (enableLegacyClientAuth) {
            addEnv("EnableLegacyClientAuth", "true");
        }

        if (enableWireCrypt) {
            addEnv("EnableWireCrypt", "true");
        } else if (!isWireEncryptionSupported()) {
            log.warn("Java Virtual Machine does not support wire protocol encryption requirements. " +
                "Downgrading to EnableWireCrypt = true. To fix this, configure the JVM with unlimited strength Cryptographic Jurisdiction Policy.");
            addEnv("EnableWireCrypt", "true");
        }
    }

    @Override
    public String getDriverClassName() {
        return "org.firebirdsql.jdbc.FBDriver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:firebirdsql://" + getContainerIpAddress() + ":" + getMappedPort(FIREBIRD_PORT) + "/" + databaseName;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
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
    protected String getTestQueryString() {
        return "select 1 from RDB$DATABASE";
    }

    @Override
    public SELF withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    @Override
    public SELF withUsername(final String username) {
        this.username = username;
        return self();
    }

    @Override
    public SELF withPassword(final String password) {
        this.password = password;
        return self();
    }

    /**
     * Enables legacy authentication plugin ({@code legacy_auth}) and use it as the default.
     *
     * @return this container
     */
    public SELF withEnableLegacyClientAuth() {
        this.enableLegacyClientAuth = true;
        return self();
    }

    /**
     * Relax wireCrypt setting from Required to Enabled.
     *
     * @return this container
     */
    public SELF withEnableWireCrypt() {
        this.enableWireCrypt = true;
        return self();
    }

    /**
     * Set the time zone of the image, defaults to the JVM default zone.
     *
     * @param timeZone Time zone name (prefer long names like Europe/Amsterdam)
     * @return this container
     */
    public SELF withTimeZone(final String timeZone) {
        this.timeZone = timeZone;
        return self();
    }

    /**
     * Set the sysdba password.
     * <p>
     * If {@code username} is {@code "sysdba"} (case insensitive), then {@code password} is used instead.
     * </p>
     *
     * @param sysdbaPassword Sysdba password
     * @return this container
     */
    public SELF withSysdbaPassword(final String sysdbaPassword) {
        this.sysdbaPassword = sysdbaPassword;
        return self();
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }

    /**
     * Checks if the JVM meets the Jaybird (Firebird JDBC driver) requirements for encrypted connections.
     * <p>
     * Specifically, this checks if the ARC4 cipher can be used with 160 bit keys.
     * </p>
     *
     * @return {@code true} if Jaybird will be able to support encrypted connections on this JVM.
     */
    public static boolean isWireEncryptionSupported() {
        try {
            return Cipher.getMaxAllowedKeyLength("ARC4") >= ARC4_REQUIRED_BITS;
        } catch (NoSuchAlgorithmException e) {
            log.error("Cipher not found, JVM doesn't support encryption requirements", e);
            return false;
        }
    }
}
