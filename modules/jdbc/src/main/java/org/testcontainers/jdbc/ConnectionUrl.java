package org.testcontainers.jdbc;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This is an Immutable class holding JDBC Connection Url and its parsed components, used by {@link ContainerDatabaseDriver}.
 * <p>
 * {@link ConnectionUrl#parseUrl()} method must be called after instantiating this class.
 *
 * @author manikmagar
 */
public class ConnectionUrl {

    @Getter
    private String url;

    private String databaseType;

    @Getter
    private String imageTag = "latest";

    private String dbHostString;

    @Getter
    private boolean inDaemonMode = false;

    @Getter
    private Optional<String> databaseHost = Optional.empty();

    @Getter
    private Optional<Integer> databasePort = Optional.empty();

    @Getter
    private Optional<String> databaseName = Optional.empty();

    @Getter
    private Optional<String> initScriptPath = Optional.empty();

    @Getter
    private Optional<InitFunctionDef> initFunction = Optional.empty();

    @Getter
    private Optional<String> queryString;

    private ConnectionUrl() {
        //Not Allowed here
    }

    public ConnectionUrl(final String url) {
        this.url = Objects.requireNonNull(url, "Connection URL cannot be null");
    }

    public String getDatabaseType() {
        return Objects.requireNonNull(this.databaseType, "Database Type cannot be null. Have you called parseUrl() method?");
    }


    /**
     * This is a part of the connection string that may specify host:port/databasename.
     * It may vary for different clients and so clients can parse it as needed.
     *
     * @return
     */
    public String getDbHostString() {
        return Objects.requireNonNull(this.dbHostString, "Database Host String cannot be null. Have you called parseUrl() method?");
    }

    public static boolean accepts(final String url) {
        return url.startsWith("jdbc:tc:");
    }

    public void parseUrl() {
        /*
        Extract from the JDBC connection URL:
         * The database type (e.g. mysql, postgresql, ...)
         * The docker tag, if provided.
         * The URL query string, if provided
       */
        Matcher urlMatcher = Patterns.URL_MATCHING_PATTERN.matcher(this.getUrl());
        if (!urlMatcher.matches()) {
            //Try for Oracle pattern
            urlMatcher = Patterns.ORACLE_URL_MATCHING_PATTERN.matcher(this.getUrl());
            if (!urlMatcher.matches()) {
                throw new IllegalArgumentException("JDBC URL matches jdbc:tc: prefix but the database or tag name could not be identified");
            }
        }
        databaseType = urlMatcher.group(1);

        imageTag = Optional.ofNullable(urlMatcher.group(3)).orElse("latest");

        //String like hostname:port/database name, which may vary based on target database.
        //Clients can further parse it as needed.
        dbHostString = urlMatcher.group(4);

        //In case it matches to the default pattern
        Matcher dbInstanceMatcher = Patterns.DB_INSTANCE_MATCHING_PATTERN.matcher(dbHostString);
        if (dbInstanceMatcher.matches()) {
            databaseHost = Optional.of(dbInstanceMatcher.group(1));
            databasePort = Optional.ofNullable(dbInstanceMatcher.group(3)).map(value -> Integer.valueOf(value));
            databaseName = Optional.of(dbInstanceMatcher.group(4));
        }

        queryString = Optional.ofNullable(urlMatcher.group(5));
        getQueryParameters();

        Matcher matcher = Patterns.INITSCRIPT_MATCHING_PATTERN.matcher(this.getUrl());
        if (matcher.matches()) {
            initScriptPath = Optional.ofNullable(matcher.group(2));
        }

        Matcher funcMatcher = Patterns.INITFUNCTION_MATCHING_PATTERN.matcher(this.getUrl());
        if (funcMatcher.matches()) {
            initFunction = Optional.of(new InitFunctionDef(funcMatcher.group(2), funcMatcher.group(4)));
        }

        Matcher daemonMatcher = Patterns.DAEMON_MATCHING_PATTERN.matcher(this.getUrl());
        inDaemonMode = daemonMatcher.matches() ? Boolean.parseBoolean(daemonMatcher.group(2)) : false;

    }

    /**
     * Get the TestContainers Parameters such as Init Function, Init Script path etc.
     *
     * @return {@link Map}
     */
    public Map<String, String> getContainerParameters() {

        Map<String, String> results = new HashMap<>();

        Matcher matcher = Patterns.TC_PARAM_MATCHING_PATTERN.matcher(this.getUrl());
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            results.put(key, value);
        }

        return results;
    }

    /**
     * Get all Query paramters specified in the Connection URL after ?. This also includes TestContainers parameters.
     *
     * @return {@link Map}
     */
    public Map<String, String> getQueryParameters() {

        Map<String, String> results = new HashMap<>();
        StringJoiner query = new StringJoiner("&");
        Matcher matcher = Patterns.QUERY_PARAM_MATCHING_PATTERN.matcher(this.getQueryString().orElse(""));
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            if (!key.startsWith("TC_")) query.add(key + "=" + value);
            results.put(key, value);
        }

        queryString = Optional.of("?" + query.toString());
        return results;
    }

    @Override
    public boolean equals(Object obj) {
        if (Objects.isNull(obj) || !(obj instanceof ConnectionUrl)) return false;
        return this.getUrl().equals(((ConnectionUrl) obj).getUrl());
    }

    /**
     * This interface defines the Regex Patterns used by {@link ConnectionUrl}.
     *
     * @author manikmagar
     */
    public interface Patterns {
        final Pattern URL_MATCHING_PATTERN = Pattern.compile("jdbc:tc:([a-z]+)(:([^:]+))?://([^\\?]+)(\\?.*)?");

        final Pattern ORACLE_URL_MATCHING_PATTERN = Pattern.compile("jdbc:tc:([a-z]+)(:([^(thin:)]+))?:thin:@([^\\?]+)(\\?.*)?");

        //Matches to part of string - hostname:port/databasename
        final Pattern DB_INSTANCE_MATCHING_PATTERN = Pattern.compile("([^:]+)(:([0-9]+))?/([^\\\\?]+)");

        final Pattern DAEMON_MATCHING_PATTERN = Pattern.compile(".*([\\?&]?)TC_DAEMON=([^\\?&]+).*");
        final Pattern INITSCRIPT_MATCHING_PATTERN = Pattern.compile(".*([\\?&]?)TC_INITSCRIPT=([^\\?&]+).*");
        final Pattern INITFUNCTION_MATCHING_PATTERN = Pattern.compile(".*([\\?&]?)TC_INITFUNCTION=" +
            "((\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)" +
            "::" +
            "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)" +
            ".*");

        final Pattern TC_PARAM_MATCHING_PATTERN = Pattern.compile("(TC_[A-Z_]+)=([^\\?&]+)");

        final Pattern QUERY_PARAM_MATCHING_PATTERN = Pattern.compile("([^\\?&=]+)=([^\\?&]+)");

    }

    @Getter
    @AllArgsConstructor
    public class InitFunctionDef {
        private String className;
        private String methodName;
    }
}
