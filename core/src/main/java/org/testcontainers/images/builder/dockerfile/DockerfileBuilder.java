package org.testcontainers.images.builder.dockerfile;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.images.builder.dockerfile.statement.Statement;
import org.testcontainers.images.builder.dockerfile.traits.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
public class DockerfileBuilder implements DockerfileBuilderTrait<DockerfileBuilder>,
        FromStatementTrait<DockerfileBuilder>,
        AddStatementTrait<DockerfileBuilder>,
        CopyStatementTrait<DockerfileBuilder>,
        RunStatementTrait<DockerfileBuilder>,
        CmdStatementTrait<DockerfileBuilder>,
        WorkdirStatementTrait<DockerfileBuilder>,
        EnvStatementTrait<DockerfileBuilder>,
        LabelStatementTrait<DockerfileBuilder>,
        ExposeStatementTrait<DockerfileBuilder>,
        EntryPointStatementTrait<DockerfileBuilder>,
        VolumeStatementTrait<DockerfileBuilder>,
        UserStatementTrait<DockerfileBuilder> {

    private final List<Statement> statements = new ArrayList<>();

    public String build() {

        StringBuilder builder = new StringBuilder();

        for (Statement statement : statements) {
            builder.append(statement.getType());
            builder.append(" ");
            statement.appendArguments(builder);
            builder.append("\n");
        }

        String result = builder.toString();

        log.debug("Returning Dockerfile:\n{}", result);

        return result;
    }
}
