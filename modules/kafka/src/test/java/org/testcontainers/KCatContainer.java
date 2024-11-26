package org.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;

public class KCatContainer extends GenericContainer<KCatContainer> {

    public KCatContainer() {
        super("confluentinc/cp-kcat:7.4.1");
        withCreateContainerCmdModifier(cmd -> {
            cmd.withEntrypoint("sh");
        });
        withCopyToContainer(Transferable.of("Message produced by kcat"), "/data/msgs.txt");
        withCommand("-c", "tail -f /dev/null");
    }
}
