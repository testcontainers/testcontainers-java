package org.testcontainers.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.AuthConfig;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility to look up registry authentication information for an image.
 */
public class RegistryAuthLocator {

    private static final Logger log = getLogger(RegistryAuthLocator.class);

    private final AuthConfig defaultAuthConfig;
    private final File configFile;
    private final String commandPathPrefix;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @VisibleForTesting
    RegistryAuthLocator(AuthConfig defaultAuthConfig, File configFile, String commandPathPrefix) {
        this.defaultAuthConfig = defaultAuthConfig;
        this.configFile = configFile;
        this.commandPathPrefix = commandPathPrefix;
    }

    /**
     * @param defaultAuthConfig an AuthConfig object that should be returned if there is no overriding authentication
     *                          available for images that are looked up
     */
    public RegistryAuthLocator(AuthConfig defaultAuthConfig) {
        this.defaultAuthConfig = defaultAuthConfig;
        final String dockerConfigLocation = System.getenv().getOrDefault("DOCKER_CONFIG",
            System.getProperty("user.home") + "/.docker");
        this.configFile = new File(dockerConfigLocation + "/config.json");
        this.commandPathPrefix = "";
    }

    /**
     * Looks up an AuthConfig for a given image name.
     * <p>
     * Lookup is performed in following order:
     * <ol>
     *     <li>{@code auths} is checked for existing credentials for the specified registry.</li>
     *     <li>if no existing auth is found, {@code credHelpers} are checked for helper for the specified registry.</li>
     *     <li>if no suitable {@code credHelpers} found, {@code credsStore} is used.</li>
     *     <li>if no {@code credsStore} is found then the default configuration is returned.</li>
     * </ol>
     *
     * @param dockerImageName image name to be looked up (potentially including a registry URL part)
     * @return an AuthConfig that is applicable to this specific image OR the defaultAuthConfig that has been set for
     * this {@link RegistryAuthLocator}.
     */
    public AuthConfig lookupAuthConfig(DockerImageName dockerImageName) {

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
            final String reposName = dockerImageName.getRegistry();

            final AuthConfig existingAuthConfig = findExistingAuthConfig(config, reposName);
            if (existingAuthConfig != null) {
                return existingAuthConfig;
            }
            // auths is empty, using helper:
            final AuthConfig helperAuthConfig = authConfigUsingHelper(config, reposName);
            if (helperAuthConfig != null) {
                return helperAuthConfig;
            }
            // no credsHelper to use, using credsStore:
            final AuthConfig storeAuthConfig = authConfigUsingStore(config, reposName);
            if (storeAuthConfig != null) {
                return storeAuthConfig;
            }
            // otherwise, defaultAuthConfig should already contain any credentials available
        } catch (Exception e) {
            log.error("Failure when attempting to lookup auth config (dockerImageName: {}, configFile: {}. " +
                "Falling back to docker-java default behaviour",
                dockerImageName,
                configFile,
                e);
        }
        return defaultAuthConfig;
    }

    private AuthConfig findExistingAuthConfig(final JsonNode config, final String reposName) throws Exception {
        final Map.Entry<String, JsonNode> entry = findAuthNode(config, reposName);
        if (entry != null && entry.getValue() != null && entry.getValue().size() > 0) {
            return OBJECT_MAPPER
                .treeToValue(entry.getValue(), AuthConfig.class)
                .withRegistryAddress(entry.getKey());
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

    private Map.Entry<String, JsonNode> findAuthNode(final JsonNode config, final String reposName) throws Exception {
        final JsonNode auths = config.get("auths");
        if (auths != null && auths.size() > 0) {
            final Iterator<Map.Entry<String, JsonNode>> fields = auths.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> entry = fields.next();
                if (entry.getKey().endsWith("://" + reposName)) {
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
            log.error("Failure running docker credential helper ({})", credentialHelperName);
            throw e;
        }

        final JsonNode helperResponse = OBJECT_MAPPER.readTree(data);
        log.debug("Credential helper provided auth config for: {}", hostName);

        return new AuthConfig()
            .withRegistryAddress(helperResponse.at("/ServerURL").asText())
            .withUsername(helperResponse.at("/Username").asText())
            .withPassword(helperResponse.at("/Secret").asText());
    }
}
