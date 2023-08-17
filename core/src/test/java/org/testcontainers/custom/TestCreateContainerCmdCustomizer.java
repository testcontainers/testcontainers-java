package org.testcontainers.custom;

import com.github.dockerjava.api.command.CreateContainerCmd;
import org.testcontainers.core.CreateContainerCmdCustomizer;

public class TestCreateContainerCmdCustomizer implements CreateContainerCmdCustomizer {

    @Override
    public void customize(CreateContainerCmd createContainerCmd) {
        createContainerCmd.getLabels().put("project", "testcontainers-java");
    }
}
