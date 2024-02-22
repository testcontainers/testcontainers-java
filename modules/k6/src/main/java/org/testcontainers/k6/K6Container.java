package org.testcontainers.k6;

import org.apache.commons.io.FilenameUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class K6Container extends GenericContainer<K6Container> {

    /** Standard image for k6, as provided by Grafana. */
    private static final DockerImageName K6_IMAGE = DockerImageName.parse("grafana/k6");

    private String testScript;

    private List<String> cmdOptions = new ArrayList<>();

    private Map<String, String> scriptVars = new HashMap<>();

    /**
     * Creates a new container instance based upon the provided image name.
     */
    public K6Container(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * Creates a new container instance based upon the provided image.
     */
    public K6Container(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(K6_IMAGE);
    }

    /**
     * Specifies the test script to be executed within the container.
     * @param testScript file to be copied into the container
     * @return the builder
     */
    public K6Container withTestScript(MountableFile testScript) {
        this.testScript = "/home/k6/" + FilenameUtils.getName(testScript.getResolvedPath());
        withCopyFileToContainer(testScript, this.testScript);
        return self();
    }

    /**
     * Specifies additional command line options to be provided to the k6 command.
     * @param options command line options
     * @return the builder
     */
    public K6Container withCmdOptions(String... options) {
        cmdOptions.addAll(Arrays.asList(options));
        return self();
    }

    /**
     * Adds a key-value pair for access within test scripts as an environment variable.
     * @param key   unique identifier for the variable
     * @param value value of the variable
     * @return the builder
     */
    public K6Container withScriptVar(String key, String value) {
        scriptVars.put(key, value);
        return self();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        List<String> commandParts = new ArrayList<>();
        commandParts.add("run");
        commandParts.addAll(cmdOptions);
        for (Map.Entry<String, String> entry : scriptVars.entrySet()) {
            commandParts.add("--env");
            commandParts.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
        }
        commandParts.add(testScript);

        setCommand(commandParts.toArray(new String[]{}));
    }

}
