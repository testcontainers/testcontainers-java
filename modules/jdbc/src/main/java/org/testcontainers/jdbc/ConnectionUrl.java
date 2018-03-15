package org.testcontainers.jdbc;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This is an Immutable class holding JDBC Connection Url and its parsed components, used by {@link ContainerDatabaseDriver}.
 * <p>
 * {@link ConnectionUrl#parseUrl()} method must be called after instantiating this class.
 *
 * @author manikmagar
 */
@EqualsAndHashCode(of = "url") @Getter
public class ConnectionUrl {

    private String url;

    private String databaseType;

    private String imageTag = "latest";

    /**
     * This is a part of the connection string that may specify host:port/databasename.
     * It may vary for different clients and so clients can parse it as needed.
     */
    private String dbHostString;

    private boolean inDaemonMode = false;

    private Optional<String> databaseHost = Optional.empty();

    private Optional<Integer> databasePort = Optional.empty();

    private Optional<String> databaseName = Optional.empty();

    private Optional<String> initScriptPath = Optional.empty();

    private Optional<InitFunctionDef> initFunction = Optional.empty();

    private Optional<String> queryString;

    private Map<String, String> containerParameters;

    private Map<String, String> queryParameters;

    public static ConnectionUrl newInstance(final String url){
        ConnectionUrl connectionUrl = new ConnectionUrl(url);
        connectionUrl.parseUrl();
        return connectionUrl;
    }

    private ConnectionUrl(final String url) {
        this.url = Objects.requireNonNull(url, "Connection URL cannot be null");
    }

    public static boolean accepts(final String url) {
        return url.startsWith("jdbc:tc:");
    }

    /**
     * This method applies various REGEX Patterns to parse the URL associated with this instance.
     * This is called from a @{@link ConnectionUrl#newInstance(String)} static factory method to create immutable instance of {@link ConnectionUrl}.
     * To avoid mutation after class is instantiated, this method should not be publicly accessible.
     */
    private void parseUrl() {
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

        queryParameters = Collections.unmodifiableMap(
                                            parseQueryParameters(
                                                Optional.ofNullable(urlMatcher.group(5)).orElse("")));

        String query = queryParameters
                            .entrySet()
                                .stream()
                                .map(e -> e.getKey() + "=" + e.getValue())
                                .collect(Collectors.joining("&"));

        queryString = Optional.of("?" + query);

        containerParameters = Collections.unmodifiableMap(parseContainerParameters());

        initScriptPath = Optional.ofNullable(containerParameters.get("TC_INITSCRIPT"));

        Matcher funcMatcher = Patterns.INITFUNCTION_MATCHING_PATTERN.matcher(this.getUrl());
        if (funcMatcher.matches()) {
            initFunction = Optional.of(new InitFunctionDef(funcMatcher.group(2), funcMatcher.group(4)));
        }

        Matcher daemonMatcher = Patterns.DAEMON_MATCHING_PATTERN.matcher(this.getUrl());
        inDaemonMode = daemonMatcher.matches() && Boolean.parseBoolean(daemonMatcher.group(2));

    }

    /**
     * Get the TestContainers Parameters such as Init Function, Init Script path etc.
     *
     * @return {@link Map}
     */
    private Map<String, String> parseContainerParameters() {

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
     * Get all Query parameters specified in the Connection URL after ?. This DOES NOT include TestContainers (TC_*) parameters.
     *
     * @return {@link Map}
     */
    private Map<String, String> parseQueryParameters(final String queryString) {

        Map<String, String> results = new HashMap<>();
        Matcher matcher = Patterns.QUERY_PARAM_MATCHING_PATTERN.matcher(queryString);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            if(!key.matches(Patterns.TC_PARAM_NAME_PATTERN)) results.put(key, value);
        }

        return results;
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

        final String TC_PARAM_NAME_PATTERN = "(TC_[A-Z_]+)";

        final Pattern TC_PARAM_MATCHING_PATTERN = Pattern.compile(TC_PARAM_NAME_PATTERN + "=([^\\?&]+)");

        final Pattern QUERY_PARAM_MATCHING_PATTERN = Pattern.compile("([^\\?&=]+)=([^\\?&]+)");

    }

    @Getter
    @AllArgsConstructor
    public class InitFunctionDef {
        private String className;
        private String methodName;
    }
}
