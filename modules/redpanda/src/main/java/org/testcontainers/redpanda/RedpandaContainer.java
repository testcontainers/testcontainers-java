package org.testcontainers.redpanda;

import com.github.dockerjava.api.command.InspectContainerResponse;
import java.io.IOException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for Redpanda.
 */
public class RedpandaContainer extends GenericContainer<RedpandaContainer> {

    private static final String REDPANDA_FULL_IMAGE_NAME = "docker.redpanda.com/redpandadata/redpanda";

    @Deprecated
    private static final String REDPANDA_OLD_FULL_IMAGE_NAME = "docker.redpanda.com/vectorized/redpanda";

    private static final DockerImageName REDPANDA_IMAGE = DockerImageName.parse(REDPANDA_FULL_IMAGE_NAME);

    @Deprecated
    private static final DockerImageName REDPANDA_OLD_IMAGE = DockerImageName.parse(REDPANDA_OLD_FULL_IMAGE_NAME);

    private static final int REDPANDA_PORT = 9092;

    private static final int SCHEMA_REGISTRY_PORT = 8081;

    private static final String STARTER_SCRIPT = "/testcontainers_start.sh";

    public RedpandaContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public RedpandaContainer(DockerImageName imageName) {
        super(imageName);
        imageName.assertCompatibleWith(REDPANDA_OLD_IMAGE, REDPANDA_IMAGE);

        boolean isLessThanBaseVersion = new ComparableVersion(imageName.getVersionPart()).isLessThan("v22.2.1");
        if (REDPANDA_FULL_IMAGE_NAME.equals(imageName.getUnversionedPart()) && isLessThanBaseVersion) {
            throw new IllegalArgumentException("Redpanda version must be >= v22.2.1");
        }

        withExposedPorts(REDPANDA_PORT, SCHEMA_REGISTRY_PORT);
        withCreateContainerCmdModifier(cmd -> {
            cmd.withEntrypoint("sh");
        });
        waitingFor(Wait.forLogMessage(".*Started Kafka API server.*", 1));
        withCommand("-c", "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT);
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) throws IOException, InterruptedException {
        super.containerIsStarting(containerInfo);

        String command = "#!/bin/bash\n";

        command += "/usr/bin/rpk redpanda start --mode dev-container ";
        command += "--kafka-addr PLAINTEXT://0.0.0.0:29092,OUTSIDE://0.0.0.0:9092 ";
        command +=
            "--advertise-kafka-addr PLAINTEXT://127.0.0.1:29092,OUTSIDE://" + getHost() + ":" + getMappedPort(9092);

        copyFileToContainer(Transferable.of(command, 0777), STARTER_SCRIPT);
    }

    public String getBootstrapServers() {
        return String.format("PLAINTEXT://%s:%s", getHost(), getMappedPort(REDPANDA_PORT));
    }

    public String getSchemaRegistryAddress() {
        return String.format("http://%s:%s", getHost(), getMappedPort(SCHEMA_REGISTRY_PORT));
    }
}
