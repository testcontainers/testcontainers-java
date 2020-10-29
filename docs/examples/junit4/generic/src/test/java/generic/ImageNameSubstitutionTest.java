package generic;


import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

public class ImageNameSubstitutionTest {

    @Test
    public void simpleExample() {
        try (
            // directDockerHubReference {
            // Referring directly to an image on Docker Hub (mysql:8.0.22)
            final MySQLContainer<?> mysql = new MySQLContainer<>(
                DockerImageName.parse("mysql:8.0.22")
            )

            // start the container and use it for testing
            // }
        ) {
            mysql.start();
        }
    }

    @Test @Ignore
    public void substitutedExample() {
        try (
            // hardcodedMirror {
            // Referring directly to an image on a private registry - image name will vary
            final MySQLContainer<?> mysql = new MySQLContainer<>(
                DockerImageName.parse("registry.mycompany.com/mirror/mysql:8.0.22")
                               .asCompatibleSubstituteFor("mysql")
            )

            // start the container and use it for testing
            // }
        ) {
            mysql.start();
        }
    }
}
