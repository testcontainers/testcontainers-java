package org.testcontainers.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AuthConfig;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.zeroturnaround.exec.InvalidResultException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility to look up registry authentication information for an image.
 */
public class RegistryAuthLocator {

    private static final Logger log = LoggerFactory.getLogger(RegistryAuthLocator.class);

    private static final String DEFAULT_REGISTRY_NAME = "https://index.docker.io/v1/";

    private static final String DOCKER_AUTH_ENV_VAR = "DOCKER_AUTH_CONFIG";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static RegistryAuthLocator instance;

    private final String commandPathPrefix;

    private final String commandExtension;

    private final File configFile;

    private final String configEnv;

    private final Map<String, Optional<AuthConfig>> cache = new ConcurrentHashMap<>();

    /**
     * key - credential helper's name
     * value - helper's response for "credentials not found" use case
     */
    private final Map<String, String> CREDENTIALS_HELPERS_NOT_FOUND_MESSAGE_CACHE;

    @VisibleForTesting
    RegistryAuthLocator(
        File configFile,
        String configEnv,
        String commandPathPrefix,
        String commandExtension,
        Map<String, String> notFoundMessageHolderReference
    ) {
        this.configFile = configFile;
        this.configEnv = configEnv;
        this.commandPathPrefix = commandPathPrefix;
        this.commandExtension = commandExtension;

        this.CREDENTIALS_HELPERS_NOT_FOUND_MESSAGE_CACHE = notFoundMessageHolderReference;
    }

    /**
     */
    protected RegistryAuthLocator() {
        final String dockerConfigLocation = System
            .getenv()
            .getOrDefault("DOCKER_CONFIG", System.getProperty("user.home") + "/.docker");
        this.configFile = new File(dockerConfigLocation + "/config.json");
        this.configEnv = System.getenv(DOCKER_AUTH_ENV_VAR);
        this.commandPathPrefix = "";
        this.commandExtension = "";

        this.CREDENTIALS_HELPERS_NOT_FOUND_MESSAGE_CACHE = new HashMap<>();
    }

    public static synchronized RegistryAuthLocator instance() {
        if (instance == null) {
            instance = new RegistryAuthLocator();
        }

        return instance;
    }

    @VisibleForTesting
    static void setInstance(RegistryAuthLocator overrideInstance) {
        instance = overrideInstance;
    }

    /**
     * Looks up an AuthConfig for a given image name.
     * <p>
     * Lookup is performed in following order, as per
     * https://docs.docker.com/engine/reference/commandline/cli/:
     * <ol>
     *     <li>{@code credHelpers}</li>
     *     <li>{@code credsStore}</li>
     *     <li>Hard-coded Base64 encoded auth in {@code auths}</li>
     *     <li>otherwise, if no credentials have been found then behaviour falls back to docker-java's
     *     implementation</li>
     * </ol>
     *
     * @param dockerImageName image name to be looked up (potentially including a registry URL part)
     * @param defaultAuthConfig an AuthConfig object that should be returned if there is no overriding authentication available for images that are looked up
     * @return an AuthConfig that is applicable to this specific image OR the defaultAuthConfig.
     */
    public AuthConfig lookupAuthConfig(DockerImageName dockerImageName, AuthConfig defaultAuthConfig) {
        final String registryName = effectiveRegistryName(dockerImageName);
        log.debug("Looking up auth config for image: {} at registry: {}", dockerImageName, registryName);

        final Optional<AuthConfig> cachedAuth = cache.computeIfAbsent(
            registryName,
            __ -> lookupUncachedAuthConfig(registryName, dockerImageName)
        );

        if (cachedAuth.isPresent()) {
            log.debug("Cached auth found: [{}]", AuthConfigUtil.toSafeString(cachedAuth.get()));
            return cachedAuth.get();
        } else {
            log.debug(
                "No matching Auth Configs - falling back to defaultAuthConfig [{}]",
                AuthConfigUtil.toSafeString(defaultAuthConfig)
            );
            // otherwise, defaultAuthConfig should already contain any credentials available
            return defaultAuthConfig;
        }
    }

    private Optional<AuthConfig> lookupUncachedAuthConfig(String registryName, DockerImageName dockerImageName) {
        try {
            final JsonNode config = getDockerAuthConfig();
            log.debug("registryName [{}] for dockerImageName [{}]", registryName, dockerImageName);

            // use helper preferentially (per https://docs.docker.com/engine/reference/commandline/cli/)
            final AuthConfig helperAuthConfig = authConfigUsingHelper(config, registryName);
            if (helperAuthConfig != null) {
                log.debug("found helper auth config [{}]", AuthConfigUtil.toSafeString(helperAuthConfig));
                return Optional.of(helperAuthConfig);
            }
            // no credsHelper to use, using credsStore:
            final AuthConfig storeAuthConfig = authConfigUsingStore(config, registryName);
            if (storeAuthConfig != null) {
                log.debug("found creds store auth config [{}]", AuthConfigUtil.toSafeString(storeAuthConfig));
                return Optional.of(storeAuthConfig);
            }
            // fall back to base64 encoded auth hardcoded in config file
            final AuthConfig existingAuthConfig = findExistingAuthConfig(config, registryName);
            if (existingAuthConfig != null) {
                log.debug("found existing auth config [{}]", AuthConfigUtil.toSafeString(existingAuthConfig));
                return Optional.of(existingAuthConfig);
            }
        } catch (Exception e) {
            log.info(
                "Failure when attempting to lookup auth config. Please ignore if you don't have images in an authenticated registry. Details: (dockerImageName: {}, configFile: {}, configEnv: {}). Falling back to docker-java default behaviour. Exception message: {}",
                dockerImageName,
                configFile,
                DOCKER_AUTH_ENV_VAR,
                e.getMessage()
            );
        }
        return Optional.empty();
    }

    private JsonNode getDockerAuthConfig() throws Exception {
        log.debug(
            "RegistryAuthLocator has configFile: {} ({}) configEnv: {} ({}) and commandPathPrefix: {}",
            configFile,
            configFile.exists() ? "exists" : "does not exist",
            DOCKER_AUTH_ENV_VAR,
            configEnv != null ? "exists" : "does not exist",
            commandPathPrefix
        );

        if (configEnv != null) {
            log.debug("RegistryAuthLocator reading from environment variable: {}", DOCKER_AUTH_ENV_VAR);
            return OBJECT_MAPPER.readTree(configEnv);
        } else if (configFile.exists()) {
            log.debug("RegistryAuthLocator reading from configFile: {}", configFile);
            return OBJECT_MAPPER.readTree(configFile);
        }

        throw new NotFoundException(
            "No config supplied. Checked in order: " +
            configFile +
            " (file not found), " +
            DOCKER_AUTH_ENV_VAR +
            " (not set)"
        );
    }

    private AuthConfig findExistingAuthConfig(final JsonNode config, final String reposName) throws Exception {
        final Map.Entry<String, JsonNode> entry = findAuthNode(config, reposName);

        if (entry != null && entry.getValue() != null && entry.getValue().size() > 0) {
            final AuthConfig deserializedAuth = OBJECT_MAPPER
                .treeToValue(entry.getValue(), AuthConfig.class)
                .withRegistryAddress(entry.getKey());

            if (
                StringUtils.isBlank(deserializedAuth.getUsername()) &&
                StringUtils.isBlank(deserializedAuth.getPassword()) &&
                !StringUtils.isBlank(deserializedAuth.getAuth())
            ) {
                final String rawAuth = new String(Base64.getDecoder().decode(deserializedAuth.getAuth()));
                final String[] splitRawAuth = rawAuth.split(":", 2);

                if (splitRawAuth.length == 2) {
                    deserializedAuth.withUsername(splitRawAuth[0]);
                    deserializedAuth.withPassword(splitRawAuth[1]);
                }
            }

            return deserializedAuth;
        }
        return null;
    }

    private AuthConfig authConfigUsingHelper(final JsonNode config, final String reposName) throws Exception {
        final JsonNode credHelpers = config.get("credHelpers");
        if (credHelpers != null && credHelpers.size() > 0) {
            final JsonNode helperNode = credHelpers.get(reposName);
            if (helperNode != null && helperNode.isTextual()) {
                final String helper = helperNode.asText();
                return runCredentialProvider(reposName, helper);
            }
        }
        return null;
    }

    private AuthConfig authConfigUsingStore(final JsonNode config, final String reposName) throws Exception {
        final JsonNode credsStoreNode = config.get("credsStore");
        if (credsStoreNode != null && !credsStoreNode.isMissingNode() && credsStoreNode.isTextual()) {
            final String credsStore = credsStoreNode.asText();
            if (StringUtils.isBlank(credsStore)) {
                log.warn("Docker auth config credsStore field will be ignored, because value is blank");
                return null;
            }
            return runCredentialProvider(reposName, credsStore);
        }
        return null;
    }

    private Map.Entry<String, JsonNode> findAuthNode(final JsonNode config, final String reposName) {
        final JsonNode auths = config.get("auths");
        if (auths != null && auths.size() > 0) {
            final Iterator<Map.Entry<String, JsonNode>> fields = auths.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> entry = fields.next();
                if (entry.getKey().endsWith("://" + reposName) || entry.getKey().equals(reposName)) {
                    return entry;
                }
            }
        }
        return null;
    }

    private AuthConfig runCredentialProvider(String hostName, String helperOrStoreName) throws Exception {
        if (StringUtils.isBlank(hostName)) {
            log.debug("There is no point in locating AuthConfig for blank hostName. Returning NULL to allow fallback");
            return null;
        }

        final String credentialProgramName = getCredentialProgramName(helperOrStoreName);
        final CredsOutput data;

        log.debug(
            "Executing docker credential provider: {} to locate auth config for: {}",
            credentialProgramName,
            hostName
        );

        try {
            data = runCredentialProgram(hostName, credentialProgramName);
            if (data.getStderr() != null && !data.getStderr().isEmpty()) {
                final String responseErrorMsg = data.getStderr();

                if (!StringUtils.isBlank(responseErrorMsg)) {
                    String credentialsNotFoundMsg = getGenericCredentialsNotFoundMsg(credentialProgramName);
                    if (credentialsNotFoundMsg != null && credentialsNotFoundMsg.equals(responseErrorMsg)) {
                        log.info(
                            "Credential helper/store ({}) does not have credentials for {}",
                            credentialProgramName,
                            hostName
                        );

                        return null;
                    }

                    log.debug(
                        "Failure running docker credential helper/store ({}) with output '{}'",
                        credentialProgramName,
                        responseErrorMsg
                    );
                } else {
                    log.debug("Failure running docker credential helper/store ({})", credentialProgramName);
                }

                throw new InvalidResultException(data.getStderr(), null);
            }
        } catch (Exception e) {
            log.debug("Failure running docker credential helper/store ({})", credentialProgramName);
            throw e;
        }

        final JsonNode helperResponse = OBJECT_MAPPER.readTree(data.getStdout());
        log.debug("Credential helper/store provided auth config for: {}", hostName);

        final String username = helperResponse.at("/Username").asText();
        final String password = helperResponse.at("/Secret").asText();
        if ("<token>".equals(username)) {
            return new AuthConfig().withIdentityToken(password);
        } else {
            return new AuthConfig()
                .withRegistryAddress(helperResponse.at("/ServerURL").asText())
                .withUsername(username)
                .withPassword(password);
        }
    }

    private String getCredentialProgramName(String credHelper) {
        return commandPathPrefix + "docker-credential-" + credHelper + commandExtension;
    }

    private String effectiveRegistryName(DockerImageName dockerImageName) {
        final String registry = dockerImageName.getRegistry();
        if (!StringUtils.isEmpty(registry)) {
            return registry;
        }
        return StringUtils.defaultString(
            DockerClientFactory.instance().getInfo().getIndexServerAddress(),
            DEFAULT_REGISTRY_NAME
        );
    }

    private String getGenericCredentialsNotFoundMsg(String credentialHelperName) {
        if (!CREDENTIALS_HELPERS_NOT_FOUND_MESSAGE_CACHE.containsKey(credentialHelperName)) {
            String credentialsNotFoundMsg = discoverCredentialsHelperNotFoundMessage(credentialHelperName);
            if (!StringUtils.isBlank(credentialsNotFoundMsg)) {
                CREDENTIALS_HELPERS_NOT_FOUND_MESSAGE_CACHE.put(credentialHelperName, credentialsNotFoundMsg);
            }
        }

        return CREDENTIALS_HELPERS_NOT_FOUND_MESSAGE_CACHE.get(credentialHelperName);
    }

    private String discoverCredentialsHelperNotFoundMessage(String credentialHelperName) {
        // will do fake call to given credential helper to find out with which message
        // it response when there are no credentials for given hostName

        // hostName should be valid, but most probably not existing
        // IF its not enough, then should probably run 'list' command first to be sure...
        final String notExistentFakeHostName = "https://not.a.real.registry/url";

        String credentialsNotFoundMsg = null;
        try {
            CredsOutput data = runCredentialProgram(notExistentFakeHostName, credentialHelperName);

            if (data.getStderr() != null && !data.getStderr().isEmpty()) {
                credentialsNotFoundMsg = data.getStderr();

                log.debug(
                    "Got credentials not found error message from docker credential helper - {}",
                    credentialsNotFoundMsg
                );
            }
        } catch (Exception e) {
            log.warn(
                "Failure running docker credential helper ({}) with fake call, expected 'credentials not found' response. Exception message: {}",
                credentialHelperName,
                e.getMessage()
            );
        }

        return credentialsNotFoundMsg;
    }

    private CredsOutput runCredentialProgram(String hostName, String credentialHelperName)
        throws InterruptedException, TimeoutException, IOException {
        String[] command = SystemUtils.IS_OS_WINDOWS
            ? new String[] { "cmd", "/c", credentialHelperName, "get" }
            : new String[] { credentialHelperName, "get" };

        StringBuffer stdout = new StringBuffer();
        StringBuffer stderr = new StringBuffer();

        try {
            new ProcessExecutor()
                .command(command)
                .redirectInput(new ByteArrayInputStream(hostName.getBytes()))
                .redirectOutput(
                    new LogOutputStream() {
                        @Override
                        protected void processLine(String line) {
                            stdout.append(line).append(System.lineSeparator());
                        }
                    }
                )
                .redirectError(
                    new LogOutputStream() {
                        @Override
                        protected void processLine(String line) {
                            stderr.append(line).append(System.lineSeparator());
                        }
                    }
                )
                .exitValueNormal()
                .timeout(30, TimeUnit.SECONDS)
                .execute();
        } catch (InvalidResultException e) {}

        return new CredsOutput(stdout.toString(), stderr.toString());
    }

    static class CredsOutput {

        private final String stdout;

        private final String stderr;

        public CredsOutput(String stdout, String stderr) {
            this.stdout = stdout.trim();
            this.stderr = stderr.trim();
        }

        public String getStdout() {
            return this.stdout;
        }

        public String getStderr() {
            return this.stderr;
        }
    }
}
