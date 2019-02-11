package org.testcontainers.dockerclient.check;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * Class for environment checks (i.e. Docker version)
 *
 * @author aivinog1
 * @since 11.02.19
 */
public class DependencyChecker {

    private final boolean dockerCheckEnabled;

    public DependencyChecker(TestcontainersConfiguration testcontainersConfiguration) {
        dockerCheckEnabled = !testcontainersConfiguration.isDisableDockerVersionChecks();
    }

    /**
     * Checks docker version if <code>checkEnabled<code/>
     *
     * @param dockerVersion string that represent docker version
     */
    public void checkDockerVersion(String dockerVersion) {
        if (dockerCheckEnabled) {
            VisibleAssertions.assertThat("Docker version", dockerVersion, new BaseMatcher<String>() {
                @Override
                public boolean matches(Object o) {
                    return new ComparableVersion(o.toString()).compareTo(new ComparableVersion("1.6.0")) >= 0;
                }

                @Override
                public void describeTo(Description description) {
                    description.appendText("should be at least 1.6.0");
                }
            });
        }
    }
}
