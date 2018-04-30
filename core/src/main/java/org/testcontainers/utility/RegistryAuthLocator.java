package org.testcontainers.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.AuthConfig;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.ByteArrayInputStream;
import java.io.File;
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
        this.configFile = new File(System.getProperty("user.home") + "/.docker/config.json");
        this.commandPathPrefix = "";
    }

    /**
     * Looks up an AuthConfig for a given image name.
     *
     * @param dockerImageName image name to be looked up (potentially including a registry URL part)
     * @return an AuthConfig that is applicable to this specific image OR the defaultAuthConfig that has been set for
     * this {@link RegistryAuthLocator}.
     */
    public AuthConfig lookupAuthConfig(DockerImageName dockerImageName) {
        log.debug("Looking up auth config for image: {}", dockerImageName);
        try {
            final JsonNode config = OBJECT_MAPPER.readTree(configFile);

            final String reposName = dockerImageName.getRegistry();
            final JsonNode auths = config.at("/auths/" + reposName);

            if (!auths.isMissingNode() && auths.size() == 0) {
                // auths/<registry> is an empty dict - use a credential helper
                return authConfigUsingCredentialsStoreOrHelper(reposName, config);
            }
        } catch (Exception e) {
            log.error("Failure when attempting to lookup auth config. Falling back to docker-java default behaviour", e);
        }
        return defaultAuthConfig;
    }

    private AuthConfig authConfigUsingCredentialsStoreOrHelper(String hostName, JsonNode config) throws Exception {

        final String credsStoreName = config.at("/credsStore").asText();
        final String credHelper = config.at("/credHelpers/" + hostName).asText();

        if (StringUtils.isNotBlank(credHelper)) {
            return runCredentialProvider(hostName, credHelper);
        } else if (StringUtils.isNotBlank(credsStoreName)) {
            return runCredentialProvider(hostName, credsStoreName);
        } else {
            throw new UnsupportedOperationException();
        }
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
