package org.testcontainers.containers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.testcontainers.images.ParsedDockerfile;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Representation of a docker-compose file, with partial parsing for validation and extraction of a minimal set of
 * data.
 */
@Slf4j
@EqualsAndHashCode
class ParsedDockerComposeFile {

    private final Map<String, Object> composeFileContent;

    private final String composeFileName;

    private final File composeFile;

    @Getter
    private final Map<String, Set<String>> serviceNameToImageNames = new HashMap<>();

    ParsedDockerComposeFile(File composeFile) {
        // The default is 50 and a big docker-compose.yml file can easily go above that number. 1,000 should give us some room
        LoaderOptions options = new LoaderOptions();
        options.setMaxAliasesForCollections(1_000);
        DumperOptions dumperOptions = new DumperOptions();

        SafeConstructor constructor = new SafeConstructor(options) {
            @Override
            protected Object constructObject(Node node) {
                if (node.getTag().equals(new Tag("!reset"))) {
                    return null;
                }
                return super.constructObject(node);
            }
        };
        Yaml yaml = new Yaml(constructor, new Representer(dumperOptions), dumperOptions, options, new Resolver());
        try (FileInputStream fileInputStream = FileUtils.openInputStream(composeFile)) {
            composeFileContent = yaml.load(fileInputStream);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse YAML file from " + composeFile.getAbsolutePath(), e);
        }
        this.composeFileName = composeFile.getAbsolutePath();
        this.composeFile = composeFile;
        parseAndValidate();
    }

    @VisibleForTesting
    ParsedDockerComposeFile(Map<String, Object> testContent) {
        this.composeFileContent = testContent;
        this.composeFileName = "";
        this.composeFile = new File(".");

        parseAndValidate();
    }

    private void parseAndValidate() {
        final Map<String, ?> servicesMap;
        if (composeFileContent.containsKey("version") && "2.0".equals(composeFileContent.get("version"))) {
            log.warn(
                "Testcontainers may not be able to clean up networks spawned using Docker Compose v2.0 files. " +
                "Please see https://github.com/testcontainers/moby-ryuk/issues/2, and specify 'version: \"2.1\"' or " +
                "higher in {}",
                composeFileName
            );
        }

        if (composeFileContent.containsKey("services")) {
            final Object servicesElement = composeFileContent.get("services");
            if (servicesElement == null) {
                log.debug(
                    "Compose file {} has an unknown format: 'version' is set but 'services' is not defined",
                    composeFileName
                );
                return;
            }
            if (!(servicesElement instanceof Map)) {
                log.debug("Compose file {} has an unknown format: 'services' is not Map", composeFileName);
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, ?> temp = (Map<String, ?>) servicesElement;
            servicesMap = temp;
        } else {
            servicesMap = composeFileContent;
        }

        for (Map.Entry<String, ?> entry : servicesMap.entrySet()) {
            String serviceName = entry.getKey();
            Object serviceDefinition = entry.getValue();
            if (!(serviceDefinition instanceof Map)) {
                log.debug(
                    "Compose file {} has an unknown format: service '{}' is not Map",
                    composeFileName,
                    serviceName
                );
                break;
            }

            @SuppressWarnings("unchecked")
            final Map<String, ?> serviceDefinitionMap = (Map<String, ?>) serviceDefinition;

            validateNoContainerNameSpecified(serviceName, serviceDefinitionMap);
            findServiceImageName(serviceName, serviceDefinitionMap);
            findImageNamesInDockerfile(serviceName, serviceDefinitionMap);
        }
    }

    private void validateNoContainerNameSpecified(String serviceName, Map<String, ?> serviceDefinitionMap) {
        if (serviceDefinitionMap.containsKey("container_name")) {
            throw new IllegalStateException(
                String.format(
                    "Compose file %s has 'container_name' property set for service '%s' but this property is not supported by Testcontainers, consider removing it",
                    composeFileName,
                    serviceName
                )
            );
        }
    }

    private void findServiceImageName(String serviceName, Map<String, ?> serviceDefinitionMap) {
        Object result = serviceDefinitionMap.get("image");
        if (result instanceof String) {
            final String rawImageName = (String) result;
            final String imageName = substituteEnvironmentVariables(rawImageName);
            log.debug("Resolved dependency image for Docker Compose in {}: {}", composeFileName, imageName);
            serviceNameToImageNames.put(serviceName, Sets.newHashSet(imageName));
        }
    }

    private void findImageNamesInDockerfile(String serviceName, Map<String, ?> serviceDefinitionMap) {
        final Object buildNode = serviceDefinitionMap.get("build");
        Path dockerfilePath = null;

        if (buildNode instanceof Map) {
            final Map<?, ?> buildElement = (Map<?, ?>) buildNode;
            final Object dockerfileRelativePath = buildElement.get("dockerfile");
            final Object contextRelativePath = buildElement.get("context");
            if (dockerfileRelativePath instanceof String && contextRelativePath instanceof String) {
                dockerfilePath =
                    composeFile
                        .getAbsoluteFile()
                        .getParentFile()
                        .toPath()
                        .resolve((String) contextRelativePath)
                        .resolve((String) dockerfileRelativePath)
                        .normalize();
            }
        } else if (buildNode instanceof String) {
            dockerfilePath =
                composeFile
                    .getAbsoluteFile()
                    .getParentFile()
                    .toPath()
                    .resolve((String) buildNode)
                    .resolve("./Dockerfile")
                    .normalize();
        }

        if (dockerfilePath != null && Files.exists(dockerfilePath)) {
            Set<String> resolvedImageNames = new ParsedDockerfile(dockerfilePath).getDependencyImageNames();
            if (!resolvedImageNames.isEmpty()) {
                log.debug(
                    "Resolved Dockerfile dependency images for Docker Compose in {} -> {}: {}",
                    composeFileName,
                    dockerfilePath,
                    resolvedImageNames
                );
                this.serviceNameToImageNames.put(serviceName, resolvedImageNames);
            }
        }
    }

    /**
     * Substitutes environment variables in a string following Docker Compose variable substitution rules.
     * Supports patterns like ${VAR}, ${VAR:-default}, and $VAR.
     * 
     * @param text the text containing variables to substitute
     * @return the text with variables substituted with their environment values
     */
    private String substituteEnvironmentVariables(String text) {
        if (text == null) {
            return null;
        }

        // Pattern for ${VAR} or ${VAR:-default} or ${VAR-default}
        Pattern bracedPattern = Pattern.compile("\\$\\{([^}]+)\\}");
        // Pattern for $VAR (word characters only)
        Pattern simplePattern = Pattern.compile("\\$([a-zA-Z_][a-zA-Z0-9_]*)");

        String result = text;

        // Handle ${VAR} and ${VAR:-default} patterns first
        Matcher bracedMatcher = bracedPattern.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (bracedMatcher.find()) {
            String varExpression = bracedMatcher.group(1);
            String replacement = expandVariableExpression(varExpression);
            bracedMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        bracedMatcher.appendTail(sb);
        result = sb.toString();

        // Handle $VAR patterns
        Matcher simpleMatcher = simplePattern.matcher(result);
        sb = new StringBuffer();
        while (simpleMatcher.find()) {
            String varName = simpleMatcher.group(1);
            String value = getVariableValue(varName);
            if (value != null) {
                simpleMatcher.appendReplacement(sb, Matcher.quoteReplacement(value));
            } else {
                simpleMatcher.appendReplacement(sb, Matcher.quoteReplacement(simpleMatcher.group(0)));
            }
        }
        simpleMatcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Gets the value of a variable, checking environment variables first, then system properties.
     */
    private String getVariableValue(String varName) {
        String value = System.getenv(varName);
        if (value == null) {
            value = System.getProperty(varName);
        }
        return value;
    }

    /**
     * Expands a variable expression that may contain default values.
     * Handles formats like "VAR", "VAR:-default", and "VAR-default".
     */
    private String expandVariableExpression(String expression) {
        // Check for default value patterns
        if (expression.contains(":-")) {
            // ${VAR:-default} - use default if VAR is unset or empty
            String[] parts = expression.split(":-", 2);
            String varName = parts[0];
            String defaultValue = parts.length > 1 ? parts[1] : "";
            String value = getVariableValue(varName);
            return (value != null && !value.isEmpty()) ? value : defaultValue;
        } else if (expression.contains("-")) {
            // ${VAR-default} - use default if VAR is unset (but not if empty)
            String[] parts = expression.split("-", 2);
            String varName = parts[0];
            String defaultValue = parts.length > 1 ? parts[1] : "";
            String value = getVariableValue(varName);
            return (value != null) ? value : defaultValue;
        } else {
            // Simple variable ${VAR}
            String value = getVariableValue(expression);
            return value != null ? value : "${" + expression + "}";
        }
    }
}
