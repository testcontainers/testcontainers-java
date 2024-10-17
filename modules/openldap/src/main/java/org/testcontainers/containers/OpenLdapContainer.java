package org.testcontainers.containers;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.schema.InetOrgPerson;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Testcontainers implementation for OpenLDAP.
 * <p>
 * Supported images: {@code bitnami/openldap}, {@code openldap}
 * </p>
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>LDAP: 389</li>
 *     <li>LDAPS: 636</li>
 * </ul>
 * </p>
 *
 * <h3>Additional functionality</h3>
 * <p>
 * This class provides additional functionality intended to simply writing tests.
 * The first is a collection of methods to acquire a connection to the server:
 * <ul>
 *     <li>connectAnonymously()</li>
 *     <li>connectAsAdmin()</li>
 *     <li>connectAsConfig()</li>
 *     <li>connectAsUser(String username, String password)</li>
 * </ul>
 * </p>
 * <p>
 * The second is a collection of methods to retrieve standard information from
 * the server:
 * <ul>
 *     <li>listObjectClasses()</li>
 * </ul>
 * </p>
 * <p>
 * Analogous methods are provided for any LDAP connection, although it's important
 * to remember that servers have different implementations. E.g., 'schemas' vs
 * 'subschemas'.
 * <ul>
 *     <li>listObjectClasses(DirContext ctx)</li>
 *     <li>listUsers(DirContext ctx, String query)</li>
 *     <li>getUserDetails(DirContext ctx, String query)</li>
 * </ul>
 * </p>
 *
 * <h3>Limitations</h3>
 * <p>
 * This implementation does not support the following features of the
 * underlying Bitnami docker container:
 * <ul>
 *     <li>AccessLog Module</li>
 *     <li>Syncrepl Module</li>
 *     <li>Proxy Protocol Support</li>
 * </ul>
 * </p>
 *
 * <h3>Extensions</h3>
 * <p>
 * <ul>
 * <li>Additional certs should be added to /opt/bitnami/openldap/certs'</li>
 * <li>Custom initialization scripts can be added to '/docker-entrypoint-initdb.d/'</li>
 * <li>Persistence can be supported with '/bitnami/openldap/'</li>
 * <li>The json log driver is used by default. It can be modified with '--log-driver'</li>
 * </ul>
 * </p>
 */
public class OpenLdapContainer extends GenericContainer<OpenLdapContainer> {
    private static final Logger LOG = LoggerFactory.getLogger(OpenLdapContainer.class);

    private static final String LDAP_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";

    private static final String LDAP_PROTOCOL_DEFAULT = "ldap";

    private static final String LDAP_PROTOCOL_SECURED = "ldaps";

    private static final String LDAP_AUTHENTICATION_ANONYMOUS = "none";

    // provide username but no password - not sure if this is the correct term
    // private static final String LDAP_AUTHENTICATION_UNAUTHENTICATED = "unauthenticated";

    private static final String LDAP_AUTHENTICATION_SIMPLE = "simple";

    // advanced techniques. May need to specify something more precise, e.g., "kerberos"
    // private static final String LDAP_AUTHENTICATION_SASL = "sasl";

    // default values used by Bitnami docker image
    private static final Boolean DEFAULT_ALLOW_ANON_BINDING = Boolean.FALSE;

    private static final Boolean DEFAULT_ALLOW_EMPTY_PASSWORD = Boolean.FALSE;

    private static final Boolean DEFAULT_BITNAMI_DEBUG = Boolean.FALSE;

    private static final String DEFAULT_BASE_DN = "dc=example,dc=org";

    private static final String DEFAULT_ADMIN_USERNAME = "admin";

    private static final String DEFAULT_ADMIN_PASSWORD = "adminpassword";

    private static final Boolean DEFAULT_CONFIG_ADMIN_ENABLED = Boolean.FALSE;

    private static final String DEFAULT_CONFIG_USERNAME = "admin";

    private static final String DEFAULT_CONFIG_PASSWORD = "configpassword";

    private static final String DEFAULT_USER_DC = "users";

    private static final String DEFAULT_GROUP = "readers";

    private static final Boolean DEFAULT_ENABLE_TLS = Boolean.FALSE;

    private static final Boolean DEFAULT_REQUIRE_TLS = Boolean.FALSE;

    // default image name
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("bitnami/openldap");

    // default tag (current as this module is written)
    private static final String DEFAULT_TAG = "2.5.18";

    // default value used by the Bitnami docker container
    private static final int LDAP_PORT = 1389;

    // default value used by the Bitnami docker container
    private static final int LDAPS_PORT = 1636;

    // default schameas: cosine,inetorgperson,nis

    // https://hub.docker.com/r/bitnami/openldap

    // LDAP_USERS: Comma separated list of LDAP users to create in the default LDAP tree. Default: user01,user02
    // LDAP_PASSWORDS: Comma separated list of passwords to use for LDAP users. Default: bitnami1,bitnami2
    //
    // LDAP_ADD_SCHEMAS: Whether to add the schemas specified in LDAP_EXTRA_SCHEMAS. Default: yes
    // LDAP_EXTRA_SCHEMAS: Extra schemas to add, among OpenLDAP's distributed schemas. Default: cosine, inetorgperson, nis
    // LDAP_SKIP_DEFAULT_TREE: Whether to skip creating the default LDAP tree based on LDAP_USERS, LDAP_PASSWORDS, LDAP_USER_DC and LDAP_GROUP. Please note that this will not skip the addition of schemas or importing of LDIF files. Default: no
    // LDAP_CUSTOM_LDIF_DIR: Location of a directory that contains LDIF files that should be used to bootstrap the database. Only files ending in .ldif will be used. Default LDAP tree based on the LDAP_USERS, LDAP_PASSWORDS, LDAP_USER_DC and LDAP_GROUP will be skipped when LDAP_CUSTOM_LDIF_DIR is used. When using this it will override the usage of LDAP_USERS, LDAP_PASSWORDS, LDAP_USER_DC and LDAP_GROUP. You should set LDAP_ROOT to your base to make sure the olcSuffix configured on the database matches the contents imported from the LDIF files. Default: /ldifs
    // LDAP_CUSTOM_SCHEMA_FILE: Location of a custom internal schema file that could not be added as custom ldif file (i.e. containing some structuralObjectClass). Default is /schema/custom.ldif"
    // LDAP_CUSTOM_SCHEMA_DIR: Location of a directory containing custom internal schema files that could not be added as custom ldif files (i.e. containing some structuralObjectClass). This can be used in addition to or instead of LDAP_CUSTOM_SCHEMA_FILE (above) to add multiple schema files. Default: /schemas

    // LDAP_TLS_CERT_FILE=/opt/bitnami/openldap/certs/openldap.crt
    // LDAP_TLS_KEY_FILE=/opt/bitnami/openldap/certs/openldap.key
    // LDAP_TLS_CA_FILE=/opt/bitnami/openldap/certs/openldapCA.crt

    private final OpenLdapConfiguration configuration;

    /**
     * Default constructor
     */
    public OpenLdapContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * Standard constructor
     *
     * @param dockerImageName Docker image compatible with 'bitnami:openldap'
     */
    public OpenLdapContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        this.waitStrategy =
            new LogMessageWaitStrategy()
                .withRegEx(".*[0-9a-f]{8}.+slapd starting.*\\s")
                .withStartupTimeout(Duration.of(10, ChronoUnit.SECONDS));
        this.configuration = new OpenLdapConfiguration();
        // - in case we need something like this...
        // super.withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withCapAdd(Capability.IPC_LOCK));
    }

    /**
     * Specify whether to allow anonymous binding
     *
     * @param allowAnonBinding true if anonymous binding should be allowed. (Default: true)
     * @return self
     */
    public OpenLdapContainer withAllowAnonBinding(Boolean allowAnonBinding) {
        configuration.setAllowAnonBinding(allowAnonBinding);
        return self();
    }

    /**
     * Specify whether empty passwords are allowed
     *
     * @param allowEmptyPassword true if empty passwords are allowed. (Default: false)
     * @return self
     */
    public OpenLdapContainer withAllowEmptyPassword(Boolean allowEmptyPassword) {
        configuration.setAllowEmptyPassword(allowEmptyPassword);
        return self();
    }

    /**
     * Specify whether container-based debugging should be enabled
     *
     * @param containerDebuggingEnabled true if container-based debugging should be enabled. (Default: false)
     * @return self
     */
    public OpenLdapContainer withContainerDebuggingEnabled(Boolean containerDebuggingEnabled) {
        configuration.setContainerDebuggingEnabled(containerDebuggingEnabled);
        return self();
    }

    /**
     * Specify Base Distinguished Name
     *
     * @param baseDN Base Distinguished Name (No default value)
     * @return self
     */
    public OpenLdapContainer withBaseDN(String baseDN) {
        configuration.setBaseDN(baseDN);
        return self();
    }

    /**
     * Specify administrative username and password
     *
     * @param username administrative user name (default: 'admin')
     * @param password administrative user password (default: 'adminpassword')
     * @return self
     */
    public OpenLdapContainer withAdminUsername(String username, String password) {
        configuration.setAdminUsername(username);
        configuration.setAdminPassword(password);
        return self();
    }

    /**
     * Specify configuration administrative username and password
     *
     * @param username configuration administrative user name (default: 'admin')
     * @param password configuration administrative user password (default: 'adminpassword')
     * @return self
     */
    public OpenLdapContainer withConfigAdminUsername(String username, String password) {
        configuration.setConfigAdminEnabled(Boolean.TRUE);
        configuration.setConfigAdminUsername(username);
        configuration.setConfigAdminPassword(password);
        return self();
    }

    /**
     * Specify whether to enable TLS
     *
     * @param enableTls true to enable TLS (default: false)
     * @return self
     */
    public OpenLdapContainer withEnableTls(Boolean enableTls) {
        configuration.setEnableTls(enableTls);
        return self();
    }

    /**
     * Specify whether to require TLS
     *
     * @param requireTls true to require TLS (default: false)
     * @return self
     */
    public OpenLdapContainer withRequireTls(Boolean requireTls) {
        configuration.setEnableTls(requireTls);
        return self();
    }

    /**
     * Specify user domain component
     *
     * @param userDc user domain component (default: 'users')
     * @return self
     */
    public OpenLdapContainer withUserDc(String userDc) {
        configuration.setUserDC(userDc);
        return self();
    }

    /**
     * Specify name of group attribute
     *
     * @param group name of group attribute (default: 'readers')
     * @return self
     */
    public OpenLdapContainer withGroup(String group) {
        configuration.setGroup(group);
        return self();
    }

    /**
     * Is anonymous binding permitted?
     *
     * @return true if anonymous binding is permitted
     */
    public Boolean getAllowAnonBinding() {
        if (configuration.getAllowAnonBinding() != null) {
            return configuration.getAllowAnonBinding();
        }
        return DEFAULT_ALLOW_ANON_BINDING;
    }

    /**
     * Are empty passwords allowed?
     *
     * @return true if empty passwords are allowed
     */
    public Boolean getAllowEmptyPassword() {
        if (configuration.getAllowEmptyPassword() != null) {
            return configuration.getAllowEmptyPassword();
        }
        return DEFAULT_ALLOW_EMPTY_PASSWORD;
    }

    /**
     * Is container debugging enabled?
     *
     * @return true if container debugging is enabled
     */
    public Boolean getContainerDebuggingEnabled() {
        if (configuration.getContainerDebuggingEnabled()) {
            return configuration.getContainerDebuggingEnabled();
        }
        return DEFAULT_BITNAMI_DEBUG;
    }

    /**
     * Get BaseDN
     *
     * @return baseDN. May be null.
     */
    public String getBaseDN() {
        if (StringUtils.isNotBlank(configuration.getBaseDN())) {
            return configuration.getBaseDN();
        }
        return DEFAULT_BASE_DN;
    }

    /**
     * Get administrative user name
     *
     * @return administrative user name
     */
    public String getAdminUsername() {
        if (StringUtils.isNotBlank(configuration.getAdminUsername())) {
            return configuration.getAdminUsername();
        }
        return DEFAULT_ADMIN_USERNAME;
    }

    /**
     * Get administrative user password
     *
     * @return administrative user password
     */
    public String getAdminPassword() {
        if (StringUtils.isNotBlank(configuration.getAdminPassword())) {
            return configuration.getAdminPassword();
        }
        return DEFAULT_ADMIN_PASSWORD;
    }

    /**
     * Is configuration administrator enabled?
     *
     * @return true if configuration administration is enabled
     */
    public Boolean getConfigAdminEnabled() {
        if (configuration.getConfigAdminEnabled() != null) {
            return configuration.getConfigAdminEnabled();
        }
        return DEFAULT_CONFIG_ADMIN_ENABLED;
    }

    /**
     * Get configuration administrative user name
     *
     * @return configuration administrative user name
     */
    public String getConfigUsername() {
        if (StringUtils.isNotBlank(configuration.getConfigAdminUsername())) {
            return configuration.getConfigAdminUsername();
        }
        return DEFAULT_CONFIG_USERNAME;
    }

    /**
     * Get configuration administrative user password
     *
     * @return configuration administrative user password
     */
    public String getConfigPassword() {
        if (StringUtils.isNotBlank(configuration.getConfigAdminPassword())) {
            return configuration.getConfigAdminPassword();
        }
        return DEFAULT_CONFIG_PASSWORD;
    }

    /**
     * Get user domain component
     *
     * @return user domain component
     */
    public String getUserDC() {
        if (StringUtils.isNotBlank(configuration.getUserDC())) {
            return configuration.getUserDC();
        }
        return DEFAULT_USER_DC;
    }

    /**
     * Get name of group attribute
     * @return name of group attribute
     */
    public String getGroup() {
        if (StringUtils.isNotBlank(configuration.getGroup())) {
            return configuration.getGroup();
        }
        return DEFAULT_GROUP;
    }

    /**
     * Get remapped LDAP (389) port
     *
     * @return remapped LDAP port, if available
     */
    public int getLdapPort() {
        return getMappedPort(LDAP_PORT);
    }

    /**
     * Get remapped LDAPS (636) port
     *
     * @return remapped LDAPS port, if available
     */
    public int getLdapsPort() {
        return getMappedPort(LDAPS_PORT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SneakyThrows
    protected void configure() {

        if (configuration.getAllowEmptyPassword() != null) {
            addEnv("ALLOW_EMPTY_PASSWORD", configuration.getAllowEmptyPassword().toString());
        }

        if (configuration.getContainerDebuggingEnabled() != null) {
            addEnv("BITNAMI_DEBUG", configuration.getContainerDebuggingEnabled().toString());
        }

        if (configuration.getAllowAnonBinding() != null) {
            addEnv("LDAP_ALLOW_ANON_BINDING", configuration.getAllowAnonBinding().toString());
        }

        if (StringUtils.isNotBlank(configuration.getBaseDN())) {
            addEnv("LDAP_ROOT", configuration.getBaseDN());
        }

        if (StringUtils.isNotBlank(configuration.getAdminUsername())) {
            addEnv("LDAP_ADMIN_USERNAME", configuration.getAdminUsername());
        }

        if (StringUtils.isNotBlank(configuration.getAdminPassword())) {
            addEnv("LDAP_ADMIN_PASSWORD", configuration.getAdminPassword());
        }

        if (Boolean.TRUE.equals(configuration.getConfigAdminEnabled())) {
            addEnv("LDAP_CONFIG_ADMIN_ENABLED", configuration.getConfigAdminEnabled().toString());
            if (StringUtils.isNotBlank(configuration.getConfigAdminUsername())) {
                addEnv("LDAP_CONFIG_ADMIN_USERNAME", configuration.getConfigAdminUsername());
            }

            if (StringUtils.isNotBlank(configuration.getConfigAdminPassword())) {
                addEnv("LDAP_CONFIG_ADMIN_PASSWORD", configuration.getConfigAdminPassword());
            }
        }

        if (StringUtils.isNotBlank(configuration.getUserDC())) {
            addEnv("LDAP_USER_DC", configuration.getUserDC());
        }

        if (StringUtils.isNotBlank(configuration.getGroup())) {
            addEnv("LDAP_GROUP", configuration.getGroup());
        }

        // TODO - add users, passwords

        if (configuration.getRequireTls() != null) {}

        // TODO - add schemas

        // Add Default Ports
        // note: it's usually one or the other!
        this.addExposedPort(LDAP_PORT);
        this.addExposedPort(LDAPS_PORT);
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return new HashSet<>(getLdapPort());
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }

    // @Override
    // @SneakyThrows
    // private void containerIsStarted(InspectContainerResponse containerInfo) {
    // }

    /**
     * Get the server's URL
     * <p>
     * Implementation note: we can't use java.net.URL/URI since 'LDAP' does not have
     * a registered URL handler.
     *
     * @return server's URL
     */
    public String getUrl() {
        // TODO: know whether to use LDAP/LDAPS, and which port
        return String.format("%s://%s:%d/", "ldap", getHost(), getLdapPort());
    }

    /**
     * Get anonymous connection to the server
     *
     * @return LDAP connection
     * @throws NamingException
     */
    public DirContext connectAnonymously() throws NamingException {
        return connectAsUser(null, null, LDAP_AUTHENTICATION_ANONYMOUS);
    }


    /**
     * Get authenticated connection to the server as the admin user
     *
     * @return LDAP connection
     * @throws NamingException
     */
    public DirContext connectAsAdmin() throws NamingException {
        return connectAsUser(getAdminUsername(), getAdminPassword(), LDAP_AUTHENTICATION_SIMPLE);
    }

    /**
     * Get authenticated connection to the server as the configuration admin user
     *
     * @return LDAP connection
     * @throws NamingException
     */
    public DirContext connectAsConfigAdmin() throws NamingException {
        return connectAsUser(getConfigUsername(), getConfigPassword(), LDAP_AUTHENTICATION_SIMPLE);
    }

    /**
     * Get authenticated connection to the server as any user
     *
     * @param username username
     * @param password password
     * @return LDAP connection
     * @throws NamingException
     */
    public DirContext connectAsUser(String username, String password) throws NamingException {
        return connectAsUser(username, password, LDAP_AUTHENTICATION_SIMPLE);
    }

    /**
     * Get JNDI DirContext that points to specified LDAP server
     *
     * @param username username
     * @param password password
     * @param authentication authentication mechanism
     * @return LDAP connection
     * @throws NamingException
     */
    private DirContext connectAsUser(String username, String password,
                                     String authentication) throws NamingException {
        final Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, LDAP_CONTEXT_FACTORY);
        env.put(Context.PROVIDER_URL, getUrl());
        env.put(Context.SECURITY_AUTHENTICATION, authentication);

        if (StringUtils.isNotBlank(username)) {
            // this may not work with inetOrgPerson users - only tested with admin
            env.put(Context.SECURITY_PRINCIPAL, "cn=" + username + "," + getBaseDN());
        }

        if (StringUtils.isNotBlank(password)) {
            env.put(Context.SECURITY_CREDENTIALS, password);
        }

        return new InitialDirContext(env);
    }

    /**
     * List object classes.
     *
     * @return list of objectClasses
     * @throws NamingException error communicating with LDAP server
     * @throws NameNotFoundException if 'subschema' isn't found (may be schema)
     */
    public List<ObjectClassInformation> listObjectClasses() throws NamingException {
        DirContext context = null;
        try {
            context = connectAsAdmin();
            return listObjectClasses(context);
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }

    /**
     * List object classes.
     *
     * @param context LDAP connection
     * @return list of objectClasses
     * @throws NamingException error communicating with LDAP server
     * @throws NameNotFoundException if 'subschema' isn't found (may be schema)
     */
    public List<ObjectClassInformation> listObjectClasses(DirContext context) throws NamingException {
        final List<ObjectClassInformation> objectClasses = new ArrayList<>();

        final SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.OBJECT_SCOPE);
        searchControls.setReturningAttributes(new String[]{"objectClasses"});

        final NamingEnumeration<SearchResult> objectClassesSearchResults =
            context.search("cn=subschema", "(ObjectClass=*)", searchControls);

        while (objectClassesSearchResults.hasMoreElements()) {
            final SearchResult result = objectClassesSearchResults.next();

            final Attributes attributes = result.getAttributes();
            final Attribute attr = attributes.get("objectClasses");

            final NamingEnumeration<?> objectClassesEnumeration = attr.getAll();
            while (objectClassesEnumeration.hasMoreElements()) {
                final String objectClassesRecord = (String) objectClassesEnumeration.next();
                try {
                    objectClasses.add(ObjectClassInformation.parse(objectClassesRecord));
                } catch (IllegalArgumentException e) {
                    // sadly not unexpected...
                    LOG.warn(e.getMessage());
                }
            }
        }

        return objectClasses;
    }

    /**
     * List all users. This method returns a Map with a key of (unordered) user CN
     * and a value of all IDs.
     *
     * @param context
     * @param query query (e.g., "ou=people," + String.join("," dc))
     * @return Map of user 'cn' and respective 'ids'
     * @throws NamingException
     */
    public Map<String, List<String>> listUsers(DirContext context, String query) throws NamingException {
        final Map<String, List<String>> users = new LinkedHashMap<>();

        // connect to server.
        final SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setReturningAttributes(new String[]{"cn"});

        try {
            final NamingEnumeration<SearchResult> userSearchResultEnumeration =
                context.search(query, "(ObjectClass=inetOrgPerson)", searchControls);
            while (userSearchResultEnumeration.hasMoreElements()) {
                final List<String> ids = new ArrayList<>();

                final SearchResult userSearchResult = userSearchResultEnumeration.next();
                final Attributes entry = userSearchResult.getAttributes();
                final NamingEnumeration<String> idEnumeration = (NamingEnumeration<String>) entry.getIDs();
                while (idEnumeration.hasMoreElements()) {
                    ids.add(idEnumeration.nextElement());
                }
                users.put(entry.get("cn").toString(), ids);
            }
        } catch (NameNotFoundException e) {
            // do nothing - return empty map
        }

        return users;
    }

    /**
     * List details a user.
     *
     * @param context
     * @param dn
     * @return optional matching user
     * @throws NamingException
     */
    public Optional<InetOrgPerson> getUserDetails(DirContext context, String dn) throws NamingException {
        final List<InetOrgPerson> users = new ArrayList<>();

        final SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.OBJECT_SCOPE);

        try {
            final NamingEnumeration<SearchResult> inetOrgPersonSearchResultEnumeration =
                context.search(dn, "(ObjectClass=inetOrgPerson)", searchControls);
            while (inetOrgPersonSearchResultEnumeration.hasMoreElements()) {
                final SearchResult inetOrgPersonSearchResult = inetOrgPersonSearchResultEnumeration.next();
                final InetOrgPerson person = parseInetOrgPerson(inetOrgPersonSearchResult);
                users.add(person);
            }
        } catch (NameNotFoundException e) {
            return Optional.empty();
        }

        // there should be, at most, a single match
        if (users.size() > 1) {
            LOG.warn("multiple matches found! dn = '{}'", dn);
        }

        return Optional.of(users.get(0));
    }

    /**
     * Parse search results for individual person
     * @param personSearchResult results of query
     * @return user information
     */
    InetOrgPerson parseInetOrgPerson(SearchResult personSearchResult) {
        final InetOrgPerson person = new InetOrgPerson();

        final Attributes entry = personSearchResult.getAttributes();

        // these are required attributes
        person.setCn(entry.get("cn").toString());
        person.setSn(entry.get("sn").toString());

        /*
        final NamingEnumeration<Attribute> attrs = (NamingEnumeration<Attribute>) entry.getAll();
        while (attrs.hasMoreElements()) {
            final List<Object> attrValues = new ArrayList<>();
            final Attribute attr = attrs.nextElement();
            final NamingEnumeration<?> values = (NamingEnumeration<?>) attr.getAll();
            while (values.hasMoreElements()) {
                attrValues.add(values.nextElement());
            }
            // userDetails.put(attr.getID(), attrValues);
        }
         */

        return person;
    }
}
