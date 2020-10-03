package groups;

import net.jqwik.api.Disabled;
import net.jqwik.api.Example;
import net.jqwik.api.Group;
import net.jqwik.api.lifecycle.AfterContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jqwik.Container;
import org.testcontainers.junit.jqwik.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class GroupedContainersTest {

    private static final DockerImageName HTTPD_IMAGE = DockerImageName.parse("httpd:2.4-alpine");

    @Container
    private final GenericContainer<?> topLevelContainer = new GenericContainer<>(HTTPD_IMAGE)
        .withExposedPorts(80);

    private static GenericContainer<?> containerOfGroup1;
    private static GenericContainer<?> containerOfGroup2;

    @Example
    public void top_level_example() {
        assertThat(topLevelContainer.isRunning()).isTrue();
    }

    @AfterContainer
    static void grouped_containers_should_not_be_running() {
        assertThat(containerOfGroup1.isRunning()).isFalse();
        assertThat(containerOfGroup2.isRunning()).isFalse();
    }

    @Group
    public class Group1 {

        @Container
        private final GenericContainer<?> groupedContainer = new GenericContainer<>(HTTPD_IMAGE)
            .withExposedPorts(80);

        private String lastGroupedContainerId;

        @Example
        public void example_with_grouped_container() {
            assertThat(groupedContainer.isRunning()).isTrue();
            containerOfGroup1 = groupedContainer;
            assertThatContainerOfGroup2IsNotRunning();
            if (lastGroupedContainerId == null) {
                lastGroupedContainerId = groupedContainer.getContainerId();
            } else {
                assertThat(lastGroupedContainerId).isNotEqualTo(groupedContainer.getContainerId());
            }
        }

        @Example
        public void other_example_with_grouped_container() {
            assertThat(groupedContainer.isRunning()).isTrue();
            containerOfGroup1 = groupedContainer;
            assertThatContainerOfGroup2IsNotRunning();
            if (lastGroupedContainerId == null) {
                lastGroupedContainerId = groupedContainer.getContainerId();
            } else {
                assertThat(lastGroupedContainerId).isNotEqualTo(groupedContainer.getContainerId());
            }
        }

        private void assertThatContainerOfGroup2IsNotRunning() {
            if (containerOfGroup2 != null) {
                assertThat(containerOfGroup2.isRunning()).isFalse();
            }
        }
    }

    @Group
    public class Group2 {

        @Container
        private final GenericContainer<?> groupedContainer = new GenericContainer<>(HTTPD_IMAGE)
            .withExposedPorts(80);

        private String lastGroupedContainerId;

        @Example
        public void example_with_grouped_container() {
            assertThat(groupedContainer.isRunning()).isTrue();
            containerOfGroup2 = groupedContainer;
            assertThatContainerOfGroup1IsNotRunning();
            if (lastGroupedContainerId == null) {
                lastGroupedContainerId = groupedContainer.getContainerId();
            } else {
                assertThat(lastGroupedContainerId).isNotEqualTo(groupedContainer.getContainerId());
            }
        }

        @Example
        public void other_example_with_grouped_container() {
            assertThat(groupedContainer.isRunning()).isTrue();
            containerOfGroup2 = groupedContainer;
            assertThatContainerOfGroup1IsNotRunning();
            if (lastGroupedContainerId == null) {
                lastGroupedContainerId = groupedContainer.getContainerId();
            } else {
                assertThat(lastGroupedContainerId).isNotEqualTo(groupedContainer.getContainerId());
            }
        }

        private void assertThatContainerOfGroup1IsNotRunning() {
            if (containerOfGroup1 != null) {
                assertThat(containerOfGroup1.isRunning()).isFalse();
            }
        }
    }

    // notSharedWithSubgroup {
    @Group
    public class GroupWithSubGroup {
        @Container
        private final GenericContainer<?> groupedContainer = new GenericContainer<>(HTTPD_IMAGE)
            .withExposedPorts(80);

        @Group
        public class Subgroup {
            @Example
            @Disabled("Container of group is not running in subgroup.")
            public void grouped_container_should_be_running() {
                assertThat(groupedContainer.isRunning()).isTrue();
            }
        }
    }
   // }

    // sharedWithSubgroup {
    @Container
    private static final GenericContainer<?> sharedContainer = new GenericContainer<>(HTTPD_IMAGE)
        .withExposedPorts(80);

    @Group
    public class GroupAccssingSharedContainer {

        @Group
        public class Subgroup {
            @Example
            public void shared_container_should_be_running() {
                assertThat(sharedContainer.isRunning()).isTrue();
            }
        }
    }
    // }


}
