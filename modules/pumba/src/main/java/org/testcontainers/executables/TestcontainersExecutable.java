package org.testcontainers.executables;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.client.PumbaCommand;
import org.testcontainers.client.PumbaExecutable;

@Slf4j
class TestcontainersExecutable implements PumbaExecutable {

    @Override
    public void execute(PumbaCommand command) {
        final String evaluatedCommand = command.evaluate();
        log.info("Executing pumba container with command \"{}\"", evaluatedCommand);
        final PumbaContainer dockerizedPumba = new PumbaContainer(evaluatedCommand);
        dockerizedPumba.start();
    }
}
