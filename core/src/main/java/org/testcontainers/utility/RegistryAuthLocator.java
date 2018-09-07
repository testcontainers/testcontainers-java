package org.testcontainers.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.AuthConfig;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.slf4j.LoggerFactory.getLogger;
import static org.testcontainers.utility.AuthConfigUtil.toSafeString;

/**
 * Utility to look up registry authentication information for an image.
 */
public class RegistryAuthLocator {

    private static final Logger log = getLogger(RegistryAuthLocator.class);
    private static final String DEFAULT_REGISTRY_NAME = "index.docker.io";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static RegistryAuthLocator instance;
    private final String commandPathPrefix;
    private final File configFile;

    @VisibleForTesting
    RegistryAuthLocator(File configFile, String commandPathPrefix) {
        this.configFile = configFile;
        this.commandPathPrefix = commandPathPrefix;
    }

    /**
     */
    protected RegistryAuthLocator() {
        final String dockerConfigLocation = System.getenv().getOrDefault("DOCKER_CONFIG",
            System.getProperty("user.home") + "/.docker");
        this.configFile = new File(dockerConfigLocation + "/config.json");
        this.commandPathPrefix = "";
    }

    public synchronized static RegistryAuthLocator instance() {
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

        if (SystemUtils.IS_OS_WINDOWS) {
            log.debug("RegistryAuthLocator is not supported on Windows. Please help test or improve it and update " +
                "https://github.com/testcontainers/testcontainers-java/issues/756");
            return defaultAuthConfig;
        }

        log.debug("Looking up auth config for image: {}", dockerImageName);

        log.debug("RegistryAuthLocator has configFile: {} ({}) and commandPathPrefix: {}",
            configFile,
            configFile.exists() ? "exists" : "does not exist",
            commandPathPrefix);

        try {
            final JsonNode config = OBJECT_MAPPER.readTree(configFile);
            final String registryName = effectiveRegistryName(dockerImageName);
            log.debug("registryName [{}] for dockerImageName [{}]", registryName, dockerImageName);

            // use helper preferentially (per https://docs.docker.com/engine/reference/commandline/cli/)
            final AuthConfig helperAuthConfig = authConfigUsingHelper(config, registryName);
            if (helperAuthConfig != null) {
                log.debug("found helper auth config [{}]", toSafeString(helperAuthConfig));
                return helperAuthConfig;
            }
            // no credsHelper to use, using credsStore:
            final AuthConfig storeAuthConfig = authConfigUsingStore(config, registryName);
            if (storeAuthConfig != null) {
                log.debug("found creds store auth config [{}]", toSafeString(storeAuthConfig));
                return storeAuthConfig;
            }
            // fall back to base64 encoded auth hardcoded in config file
            final AuthConfig existingAuthConfig = findExistingAuthConfig(config, registryName);
            if (existingAuthConfig != null) {
                log.debug("found existing auth config [{}]", toSafeString(existingAuthConfig));
                return existingAuthConfig;
            }

            log.debug("no matching Auth Configs - falling back to defaultAuthConfig [{}]", toSafeString(defaultAuthConfig));
            // otherwise, defaultAuthConfig should already contain any credentials available
        } catch (Exception e) {
            log.debug("Failure when attempting to lookup auth config (dockerImageName: {}, configFile: {}. Falling back to docker-java default behaviour. Exception message: {}",
                dockerImageName,
                configFile,
                e.getMessage());
        }
        return defaultAuthConfig;
    }

    private AuthConfig findExistingAuthConfig(final JsonNode config, final String reposName) throws Exception {

        final Map.Entry<String, JsonNode> entry = findAuthNode(config, reposName);

        if (entry != null && entry.getValue() != null && entry.getValue().size() > 0) {
            final AuthConfig deserializedAuth = OBJECT_MAPPER
                .treeToValue(entry.getValue(), AuthConfig.class)
                .withRegistryAddress(entry.getKey());

            if (isBlank(deserializedAuth.getUsername()) &&
                isBlank(deserializedAuth.getPassword()) &&
                !isBlank(deserializedAuth.getAuth())) {

                final String rawAuth = new String(Base64.getDecoder().decode(deserializedAuth.getAuth()));
                final String[] splitRawAuth = rawAuth.split(":");

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
                if (entry.getKey().contains("://" + reposName) || entry.getKey().equals(reposName)) {
                    return entry;
                }
            }
        }
        return null;
    }

    private AuthConfig runCredentialProvider(String hostName, String credHelper) throws Exception {
        final String credentialHelperName = commandPathPrefix + "docker-credential-" + credHelper;
        String data;

        log.debug("Executing docker credential helper: {} to locate auth config for: {}",
            credentialHelperName, hostName);

        try {
            data = new ProcessExecutor()
                .command(credentialHelperName, "get")
                .redirectInput(new ByteArrayInputStream(hostName.getBytes()))
                .readOutput(true)
                .exitValueNormal()
                .timeout(30, TimeUnit.SECONDS)
                .execute()
                .outputUTF8()
                .trim();
        } catch (Exception e) {
            log.debug("Failure running docker credential helper ({})", credentialHelperName);
            throw e;
        }

        final JsonNode helperResponse = OBJECT_MAPPER.readTree(data);
        log.debug("Credential helper provided auth config for: {}", hostName);

        return new AuthConfig()
            .withRegistryAddress(helperResponse.at("/ServerURL").asText())
            .withUsername(helperResponse.at("/Username").asText())
            .withPassword(helperResponse.at("/Secret").asText());
    }

    private String effectiveRegistryName(DockerImageName dockerImageName) {
        return StringUtils.defaultIfEmpty(dockerImageName.getRegistry(), DEFAULT_REGISTRY_NAME);
    }
}
