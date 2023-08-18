package org.testcontainers.custom;

import com.github.dockerjava.api.command.CreateContainerCmd;
import org.testcontainers.core.CreateContainerCmdModifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TestCreateContainerCmdModifier implements CreateContainerCmdModifier {

    @Override
    public Function<CreateContainerCmd, CreateContainerCmd> modify() {
        return cmd -> {
            Map<String, String> labels = new HashMap<>();
            labels.put("project", "testcontainers-java");
            labels.put("scope", "global");
            return cmd.withLabels(labels);
        };
    }
}
