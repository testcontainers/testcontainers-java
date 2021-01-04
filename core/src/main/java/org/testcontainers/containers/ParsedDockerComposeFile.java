package org.testcontainers.containers;

import com.google.common.annotations.VisibleForTesting;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.testcontainers.images.ParsedDockerfile;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Representation of a docker-compose file, with partial parsing for validation and extraction of a minimal set of
 * data.
 */
@Slf4j
@EqualsAndHashCode
class ParsedDockerComposeFile {

    private static final Pattern ENV_SUBSTITUTION_PATTERN =
        Pattern.compile("\\$(?:\\$|\\{(?<var>[^}]*?)(?:(?<op>:-|-|:\\?|\\?)(?<default>[^}]*))?})");

    private final Map<String, Object> composeFileContent;
    private final String composeFileName;
    private final File composeFile;

    @Getter
    private Set<String> dependencyImageNames = new HashSet<>();

    ParsedDockerComposeFile(File composeFile, Map<String, String> env) {
        Yaml yaml = new Yaml();
        try (FileInputStream fileInputStream = FileUtils.openInputStream(composeFile)) {
            Object preSubstitution = yaml.load(fileInputStream);
            //noinspection unchecked
            composeFileContent = (Map<String, Object>) substituteEnv(preSubstitution, env);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse YAML file from " + composeFile.getAbsolutePath(), e);
        }
        this.composeFileName = composeFile.getAbsolutePath();
        this.composeFile = composeFile;
        parseAndValidate();
    }

    @VisibleForTesting
    ParsedDockerComposeFile(Map<String, Object> testContent, Map<String, String> env) {
        //noinspection unchecked
        this.composeFileContent = (Map<String, Object>) substituteEnv(testContent, env);
        this.composeFileName = "";
        this.composeFile = new File(".");

        parseAndValidate();
    }

    private static Object substituteEnv(Object obj, Map<String, String> env) {
        if (obj instanceof Map) {
            HashMap<Object, Object> map = new HashMap<>();

            // Don't use Collectors.toMap here, as it doesn't support nulls in the value
            ((Map<?, ?>) obj).forEach((key, value) -> map.put(
                substituteEnv(key, env),
                substituteEnv(value, env)));

            return map;
        } else if (obj instanceof List) {
            return ((List<?>) obj)
                .stream()
                .map(entry -> substituteEnv(entry, env))
                .collect(Collectors.toList());
        } else if (obj instanceof String) {
            return substituteEnv((String)obj, env);
        } else {
            return obj;
        }
    }

    private static String substituteEnv(String input, Map<String, String> env) {
        if (!input.contains("$")) {
            return input;
        }

        Matcher matcher = ENV_SUBSTITUTION_PATTERN.matcher(input);
        StringBuilder builder = new StringBuilder();
        int lastIndex = 0;

        while (matcher.find()) {
            builder.append(input, lastIndex, matcher.start());

            String var = matcher.group("var");
            String op = matcher.group("op");
            String def = matcher.group("default");
            String val = env.get(var);

            if (var == null) {
                // If no var is captured, it's an escaped $.
                builder.append('$');
            } else if (op == null || op.equals("?") || op.equals(":?")) {
                builder.append(val != null ? val : "");
            } else if (op.equals("-")) {
                builder.append(val != null ? val : def);
            } else if (op.equals(":-")) {
                builder.append(val != null && !val.isEmpty() ? val : def);
            }

            lastIndex = matcher.end();
        }

        builder.append(input, lastIndex, input.length());

        return builder.toString();
    }

    private void parseAndValidate() {
        final Map<String, ?> servicesMap;
        if (composeFileContent.containsKey("version")) {
            if ("2.0".equals(composeFileContent.get("version"))) {
                log.warn("Testcontainers may not be able to clean up networks spawned using Docker Compose v2.0 files. " +
                    "Please see https://github.com/testcontainers/moby-ryuk/issues/2, and specify 'version: \"2.1\"' or " +
                    "higher in {}", composeFileName);
            }

            final Object servicesElement = composeFileContent.get("services");
            if (servicesElement == null) {
                log.debug("Compose file {} has an unknown format: 'version' is set but 'services' is not defined", composeFileName);
                return;
            }
            if (!(servicesElement instanceof Map)) {
                log.debug("Compose file {} has an unknown format: 'services' is not Map", composeFileName);
                return;
            }

            servicesMap = (Map<String, ?>) servicesElement;
        } else {
            servicesMap = composeFileContent;
        }

        for (Map.Entry<String, ?> entry : servicesMap.entrySet()) {
            String serviceName = entry.getKey();
            Object serviceDefinition = entry.getValue();
            if (!(serviceDefinition instanceof Map)) {
                log.debug("Compose file {} has an unknown format: service '{}' is not Map", composeFileName, serviceName);
                break;
            }

            final Map serviceDefinitionMap = (Map) serviceDefinition;

            validateNoContainerNameSpecified(serviceName, serviceDefinitionMap);
            findServiceImageName(serviceDefinitionMap);
            findImageNamesInDockerfile(serviceDefinitionMap);
        }
    }

    private void validateNoContainerNameSpecified(String serviceName, Map serviceDefinitionMap) {
        if (serviceDefinitionMap.containsKey("container_name")) {
            throw new IllegalStateException(String.format(
                "Compose file %s has 'container_name' property set for service '%s' but this property is not supported by Testcontainers, consider removing it",
                composeFileName,
                serviceName
            ));
        }
    }

    private void findServiceImageName(Map serviceDefinitionMap) {
        if (serviceDefinitionMap.containsKey("image") && serviceDefinitionMap.get("image") instanceof String) {
            final String imageName = (String) serviceDefinitionMap.get("image");
            log.debug("Resolved dependency image for Docker Compose in {}: {}", composeFileName, imageName);
            dependencyImageNames.add(imageName);
        }
    }

    private void findImageNamesInDockerfile(Map serviceDefinitionMap) {
        final Object buildNode = serviceDefinitionMap.get("build");
        Path dockerfilePath = null;

        if (buildNode instanceof Map) {
            final Map buildElement = (Map) buildNode;
            final Object dockerfileRelativePath = buildElement.get("dockerfile");
            final Object contextRelativePath = buildElement.get("context");
            if (dockerfileRelativePath instanceof String && contextRelativePath instanceof String) {
                dockerfilePath = composeFile
                    .getParentFile()
                    .toPath()
                    .resolve((String) contextRelativePath)
                    .resolve((String) dockerfileRelativePath)
                    .normalize();
            }
        } else if (buildNode instanceof String) {
            dockerfilePath = composeFile
                .getParentFile()
                .toPath()
                .resolve((String) buildNode)
                .resolve("./Dockerfile")
                .normalize();
        }

        if (dockerfilePath != null && Files.exists(dockerfilePath)) {
            Set<String> resolvedImageNames = new ParsedDockerfile(dockerfilePath).getDependencyImageNames();
            if (!resolvedImageNames.isEmpty()) {
                log.debug("Resolved Dockerfile dependency images for Docker Compose in {} -> {}: {}", composeFileName, dockerfilePath, resolvedImageNames);
                this.dependencyImageNames.addAll(resolvedImageNames);
            }
        }
    }
}
