package org.testcontainers.containers;

import com.google.common.annotations.VisibleForTesting;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Representation of a docker-compose file, with partial parsing for validation and extraction of a minimal set of
 * data.
 */
@Slf4j
@EqualsAndHashCode
class ParsedDockerComposeFile {

    private Map<String, Object> composeFileContent;
    private final String composeFileName;
    private Set<String> imageNames = new HashSet<>();

    ParsedDockerComposeFile(File composeFile) {
        Yaml yaml = new Yaml();
        try (FileInputStream fileInputStream = FileUtils.openInputStream(composeFile)) {
            composeFileContent = yaml.load(fileInputStream);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse YAML file from " + composeFile.getAbsolutePath(), e);
        }
        this.composeFileName = composeFile.getAbsolutePath();

        parseAndValidate();
    }

    @VisibleForTesting
    ParsedDockerComposeFile(Map<String, Object> testContent) {
        this.composeFileContent = testContent;
        this.composeFileName = "";

        parseAndValidate();
    }

    private void parseAndValidate() {
        if (composeFileContent == null) {
            return;
        }

        Map<String, ?> map = (Map<String, ?>) composeFileContent;

        final Map<String, ?> servicesMap;
        if (map.containsKey("version")) {
            if ("2.0".equals(map.get("version"))) {
                log.warn("Testcontainers may not be able to clean up networks spawned using Docker Compose v2.0 files. " +
                    "Please see https://github.com/testcontainers/moby-ryuk/issues/2, and specify 'version: \"2.1\"' or " +
                    "higher in {}", composeFileName);
            }


            if (!map.containsKey("services")) {
                log.debug("Compose file {} has an unknown format: 'version' is set but 'services' is not defined", composeFileName);
                return;
            }
            Object services = map.get("services");
            if (!(services instanceof Map)) {
                log.debug("Compose file {} has an unknown format: 'services' is not Map", composeFileName);
                return;
            }

            servicesMap = (Map<String, ?>) services;
        } else {
            servicesMap = map;
        }

        for (Map.Entry<String, ?> entry : servicesMap.entrySet()) {
            String serviceName = entry.getKey();
            Object serviceDefinition = entry.getValue();
            if (!(serviceDefinition instanceof Map)) {
                log.debug("Compose file {} has an unknown format: service '{}' is not Map", composeFileName, serviceName);
                break;
            }

            final Map serviceDefinitionMap = (Map) serviceDefinition;
            if (serviceDefinitionMap.containsKey("container_name")) {
                throw new IllegalStateException(String.format(
                    "Compose file %s has 'container_name' property set for service '%s' but this property is not supported by Testcontainers, consider removing it",
                    composeFileName,
                    serviceName
                ));
            }
            if (serviceDefinitionMap.containsKey("image") && serviceDefinitionMap.get("image") instanceof String) {
                imageNames.add((String) serviceDefinitionMap.get("image"));
            }
        }
    }

    public Set<String> getServiceImageNames() {
        return imageNames;
    }
}
