package generic;

import generic.support.TestSpecificImageNameSubstitutor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

public class ImageNameSubstitutionTest {

    @Test
    public void simpleExample() {
        try (
            // spotless:off
            // directDockerHubReference {
            // Referring directly to an image on Docker Hub (mysql:8.0.36)
            final MySQLContainer<?> mysql = new MySQLContainer<>(
                DockerImageName.parse("mysql:8.0.36")
            )
            // start the container and use it for testing
            // }
            // spotless:on
        ) {
            mysql.start();
        }
    }

    /**
     * Note that this test uses a fake image name, which will only work because
     * {@link TestSpecificImageNameSubstitutor} steps in to override the substitution for this exact
     * image name.
     */
    @Test
    public void substitutedExample() {
        try (
            // spotless:off
            // hardcodedMirror {
            // Referring directly to an image on a private registry - image name will vary
            final MySQLContainer<?> mysql = new MySQLContainer<>(
                DockerImageName.parse("registry.mycompany.com/mirror/mysql:8.0.36")
                    .asCompatibleSubstituteFor("mysql")
            )
            // start the container and use it for testing
            // }
            // spotless:on
        ) {
            mysql.start();
        }
    }
}
