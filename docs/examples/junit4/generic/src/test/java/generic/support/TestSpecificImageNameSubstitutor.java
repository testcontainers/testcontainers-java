package generic.support;

import org.testcontainers.utility.DefaultImageNameSubstitutor;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

/**
 * An {@link ImageNameSubstitutor} which makes it possible to use fake image names in
 * {@link generic.ImageNameSubstitutionTest}. This implementation simply reverses a fake image name when presented, and
 * is hardcoded to act upon the specific fake name in that test.
 */
public class TestSpecificImageNameSubstitutor extends ImageNameSubstitutor {

    private final DefaultImageNameSubstitutor defaultImageNameSubstitutor = new DefaultImageNameSubstitutor();

    @Override
    public DockerImageName apply(final DockerImageName original) {
        if (original.equals(DockerImageName.parse("registry.mycompany.com/mirror/mysql:8.0.22"))) {
            return defaultImageNameSubstitutor.apply(DockerImageName.parse("mysql"));
        } else {
            return defaultImageNameSubstitutor.apply(original);
        }
    }

    @Override
    protected int getPriority() {
        return 1;
    }

    @Override
    protected String getDescription() {
        return TestSpecificImageNameSubstitutor.class.getSimpleName();
    }
}
