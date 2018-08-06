package org.testcontainers.utility;

import com.github.dockerjava.api.model.Container;
import lombok.experimental.UtilityClass;

import java.util.Arrays;

/**
 * @author Eugeny Karpov
 */
@UtilityClass
public class DockerUtils {

    /**
     * Check if container has specified name
     *
     * If container name starts with / (all names in com.github.dockerjava.api.model.Container.names are starting with /)
     * then omit first letter
     */
    public static boolean isContainerNameEqual(Container container, String containerName) {
        return Arrays.stream(container.getNames())
            .map(name -> name.startsWith("/") ? name.substring(1) : name)
            .anyMatch(name -> name.equals(containerName));
    }
}
