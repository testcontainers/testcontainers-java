package org.testcontainers.custom;

import com.github.dockerjava.api.command.CreateContainerCmd;
import org.testcontainers.core.CreateContainerCmdModifier;

import java.util.HashMap;
import java.util.Map;

public class TestCreateContainerCmdModifier implements CreateContainerCmdModifier {

    @Override
    public CreateContainerCmd modify(CreateContainerCmd createContainerCmd) {
        Map<String, String> labels = new HashMap<>();
        labels.put("project", "testcontainers-java");
        labels.put("scope", "global");
        createContainerCmd.getLabels().putAll(labels);
        return createContainerCmd;
    }
}
