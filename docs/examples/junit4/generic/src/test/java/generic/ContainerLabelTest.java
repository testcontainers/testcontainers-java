package generic;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

public class ContainerLabelTest {
    // single_label {
    public GenericContainer containerWithLabel = new GenericContainer(DockerImageName.parse("alpine:3.14"))
        .withLabel("key", "value");
    // }


    // multiple_labels {
    private Map<String, String> mapOfLabels = new HashMap<>();
    // populate map, e.g. mapOfLabels.put("key1", "value1");

    public GenericContainer containerWithMultipleLabels = new GenericContainer(DockerImageName.parse("alpine:3.14"))
        .withLabels(mapOfLabels);
    // }
}
