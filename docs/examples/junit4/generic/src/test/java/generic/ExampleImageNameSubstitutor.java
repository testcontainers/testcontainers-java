package generic;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

public class ExampleImageNameSubstitutor extends ImageNameSubstitutor {

    @Override
    public DockerImageName apply(DockerImageName original) {
        // convert the original name to something appropriate for
        // our build environment
        return DockerImageName.parse(
            // your code goes here - silly example of capitalising
            // the original name is shown
            original.asCanonicalNameString().toUpperCase()
        );
    }

    @Override
    protected int getPriority() {
        // the highest priority substitutor is used.
        // Use something higher than 0, which is the priority
        // of the default implementation
        return 1;
    }

    @Override
    protected String getDescription() {
        // used in logs
        return "example image name substitutor";
    }
}
