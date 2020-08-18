package org.testcontainers.containers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.dockerjava.api.command.InspectContainerResponse;

/**
 * @author Eddú Meléndez
 */
public class GCloudGenericContainer<SELF extends GCloudGenericContainer<SELF>> extends GenericContainer<SELF> {

    public static final String DEFAULT_GCLOUD_IMAGE = "gcr.io/google.com/cloudsdktool/cloud-sdk:306.0.0";

    private List<String> commands = new ArrayList<>();

    public GCloudGenericContainer(String image) {
        super(image);
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        runAdditionalCommands();
    }

    private void runAdditionalCommands() {
        this.commands.forEach(cmd -> {
            try {
                execInContainer(cmd);
            } catch (IOException | InterruptedException e) {
                logger().error("Failed to execute {}. Exception message: {}", cmd, e.getMessage());
            }
        });
    }

    public SELF withAdditionalCommands(String... cmds) {
        this.commands.addAll(Arrays.asList(cmds));
        return self();
    }

}
