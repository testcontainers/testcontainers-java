package generic;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit4.TestcontainersRule;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

public class ContainerLabelTest {

    // single_label {
    public TestcontainersRule<GenericContainer<?>> containerWithLabel = new TestcontainersRule<>(
        new GenericContainer(DockerImageName.parse("alpine:3.17")).withLabel("key", "value")
    );
    // }

    // multiple_labels {
    private Map<String, String> mapOfLabels = new HashMap<>();
    // populate map, e.g. mapOfLabels.put("key1", "value1");

    public TestcontainersRule<GenericContainer<?>> containerWithMultipleLabels = new TestcontainersRule<>(
        new GenericContainer(DockerImageName.parse("alpine:3.17")).withLabels(mapOfLabels)
    );
    // }
}
