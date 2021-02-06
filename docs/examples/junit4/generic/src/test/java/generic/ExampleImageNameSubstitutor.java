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
    protected String getDescription() {
        // used in logs
        return "example image name substitutor";
    }
}
