package org.testcontainers.junit;

import com.github.dockerjava.api.model.Container;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;


public class DockerComposeContainerWithServicesTest {

    private static final String[] listOfServices = new String[]{"redis"/*, "db"*/};

    @Rule
    public DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
        .withServices(listOfServices);

    protected DockerComposeContainer getEnvironment() {
        return environment;
    }

    @Test
    public void testGivenDockerCompose_WhenSublistOfServicesSelected_ThenOnlyThoseServicesAreStarted() {
        Set<String> runningList = new HashSet<>();

        List<Container> list = environment.listChildContainers();
        for (Container container : list) {
            for (String service : listOfServices)
                if (container.getNames()[0].contains(service)) {
                    runningList.add(service);
                }
        }
        assertEquals("List of assigned services is equal to list of running services of the docker-compose",
            listOfServices.length, runningList.size());
    }
}
