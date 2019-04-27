package org.testcontainers.utility;

import lombok.experimental.UtilityClass;
import org.testcontainers.containers.ContainerLaunchException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

@UtilityClass
public class PreconditionUtils {
    private static final String CONTAINER_NAME_OPTION = "container_name";

    public static void checkNotContainUnsupportedComposeOptions(List<File> composeFiles) {
        composeFiles.forEach(file -> checkArgument(!containUnsupportedComposeOptions(file),
            String.format("Compose file %s contains '%s' option which is not supported by container.",
                file,
                CONTAINER_NAME_OPTION)));
    }

    private static boolean containUnsupportedComposeOptions(File file) {
        try {
            return Files.lines(file.toPath())
                .anyMatch(line -> line.contains(CONTAINER_NAME_OPTION));
        } catch (IOException e) {
            throw new ContainerLaunchException(String.format("Unable to read compose file %s.", file.getName()), e);
        }
    }
}
