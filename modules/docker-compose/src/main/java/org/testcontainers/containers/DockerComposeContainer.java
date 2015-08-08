package org.testcontainers.containers;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.Container;
import org.testcontainers.utility.Base58;

import java.io.File;
import java.util.List;

import static org.testcontainers.containers.GenericContainer.BindMode.READ_ONLY;
import static org.testcontainers.containers.GenericContainer.BindMode.READ_WRITE;

/**
 * Created by rnorth on 08/08/2015.
 */
public class DockerComposeContainer extends GenericContainer {

    /**
     * Random identifier which will become part of spawned containers names, so we can shut them down
     */
    private final String identifier;

    public DockerComposeContainer(File composeFile, String command) {
        super("dduportal/docker-compose:1.3.1");

        // Create a unique identifier and tell compose
        identifier = Base58.randomString(6).toLowerCase();
        addEnv("COMPOSE_PROJECT_NAME", identifier);

        // Map the docker compose file into the container
        addEnv("COMPOSE_FILE", "/compose/" + composeFile.getAbsoluteFile().getName());
        addFileSystemBind(composeFile.getAbsoluteFile().getParentFile().getAbsolutePath(),"/compose", READ_ONLY);

        // Ensure that compose can access docker. Since the container is assumed to be running on the same machine
        //  as the docker daemon, just mapping the docker control socket is OK.
        addFileSystemBind("/var/run/docker.sock","/var/run/docker.sock", READ_WRITE);
        setCommand(command);
    }

    @Override
    public void stop() {
        super.stop();

        /**
         * Kill all service containers that were launched by compose
         */
        try {
            List<Container> containers = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers());

            for (Container container : containers) {
                for (String name : container.names()) {
                    if (name.startsWith("/" + identifier)) {
                        dockerClient.killContainer(container.id());
                        dockerClient.removeContainer(container.id());
                    }
                }
            }

        } catch (DockerException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getIdentifier() {
        return identifier;
    }
}
