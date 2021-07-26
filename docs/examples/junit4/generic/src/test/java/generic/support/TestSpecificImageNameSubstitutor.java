package generic.support;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

/**
 * An {@link ImageNameSubstitutor} which makes it possible to use fake image names in
 * {@link generic.ImageNameSubstitutionTest}. This implementation simply reverses a fake image name when presented, and
 * is hardcoded to act upon the specific fake name in that test.
 */
public class TestSpecificImageNameSubstitutor extends ImageNameSubstitutor {

    @Override
    public DockerImageName apply(final DockerImageName original) {
        if (original.equals(DockerImageName.parse("registry.mycompany.com/mirror/mysql:8.0.24"))) {
            return DockerImageName.parse("mysql");
        } else {
            return original;
        }
    }

    @Override
    protected String getDescription() {
        return TestSpecificImageNameSubstitutor.class.getSimpleName();
    }
}
