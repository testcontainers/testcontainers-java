package org.testcontainers.custom;

import com.github.dockerjava.api.command.CreateContainerCmd;
import org.testcontainers.core.CreateContainerCmdModifier;

public class TestCreateContainerCmdModifier implements CreateContainerCmdModifier {

    @Override
    public void modify(CreateContainerCmd createContainerCmd) {
        createContainerCmd.getLabels().put("project", "testcontainers-java");
    }
}
