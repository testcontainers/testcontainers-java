package org.testcontainers.junit.yugabytedb;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.junit.Test;
import org.testcontainers.containers.YugabyteDBYCQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * YugabyteDB YCQL API unit test class
 */
public class YugabyteDBYCQLTest {

    private static final String IMAGE_NAME = "yugabytedb/yugabyte:2.14.4.0-b26";

    private static final String IMAGE_NAME_2_18 = "yugabytedb/yugabyte:2.18.3.0-b75";

    private static final DockerImageName YBDB_TEST_IMAGE = DockerImageName.parse(IMAGE_NAME);

    @Test
    public void testSmoke() {
        try (
            // creatingYCQLContainer {
            final YugabyteDBYCQLContainer ycqlContainer = new YugabyteDBYCQLContainer(
                "yugabytedb/yugabyte:2.14.4.0-b26"
            )
            // }
        ) {
            // startingYCQLContainer {
            ycqlContainer.start();
            // }
            assertThat(performQuery(ycqlContainer, "SELECT release_version FROM system.local").wasApplied())
                .as("A sample test query succeeds")
                .isTrue();
        }
    }

    @Test
    public void testCustomKeyspace() {
        String key = "random";
        try (
            final YugabyteDBYCQLContainer ycqlContainer = new YugabyteDBYCQLContainer(YBDB_TEST_IMAGE)
                .withKeyspaceName(key)
        ) {
            ycqlContainer.start();
            assertThat(
                performQuery(
                    ycqlContainer,
                    "SELECT keyspace_name FROM system_schema.keyspaces where keyspace_name='" + key + "'"
                )
                    .one()
                    .getString(0)
            )
                .as("Custom keyspace creation succeeds")
                .isEqualTo(key);
        }
    }

    @Test
    public void testAuthenticationEnabled() {
        String role = "random";
        try (
            final YugabyteDBYCQLContainer ycqlContainer = new YugabyteDBYCQLContainer(YBDB_TEST_IMAGE)
                .withUsername(role)
                .withPassword(role)
        ) {
            ycqlContainer.start();
            assertThat(
                performQuery(ycqlContainer, "SELECT role FROM system_auth.roles where role='" + role + "'")
                    .one()
                    .getString(0)
            )
                .as("Keyspace login with authentication enabled succeeds")
                .isEqualTo(role);
        }
    }

    @Test
    public void testAuthenticationDisabled() {
        try (
            final YugabyteDBYCQLContainer ycqlContainer = new YugabyteDBYCQLContainer(YBDB_TEST_IMAGE)
                .withPassword("")
                .withUsername("")
        ) {
            ycqlContainer.start();
            assertThat(performQuery(ycqlContainer, "SELECT release_version FROM system.local").wasApplied())
                .as("Keyspace login with authentication disabled succeeds")
                .isTrue();
        }
    }

    @Test
    public void testInitScript() {
        String key = "random";
        try (
            final YugabyteDBYCQLContainer ycqlContainer = new YugabyteDBYCQLContainer(YBDB_TEST_IMAGE)
                .withKeyspaceName(key)
                .withUsername(key)
                .withPassword(key)
                .withInitScript("init/init_yql.sql")
        ) {
            ycqlContainer.start();
            ResultSet output = performQuery(ycqlContainer, "SELECT greet FROM random.dsql");
            assertThat(output.wasApplied()).as("Statements from a custom script execution succeeds").isTrue();
            assertThat(output.one().getString(0)).as("A record match succeeds").isEqualTo("Hello DSQL");
        }
    }

    @Test
    public void shouldStartWhenContainerIpIsUsedInWaitStrategy() {
        try (final YugabyteDBYCQLContainer ycqlContainer = new YugabyteDBYCQLContainer(IMAGE_NAME_2_18)) {
            ycqlContainer.start();
            assertThat(performQuery(ycqlContainer, "SELECT release_version FROM system.local").wasApplied()).isTrue();
        }
    }

    private ResultSet performQuery(YugabyteDBYCQLContainer ycqlContainer, String cql) {
        try (
            CqlSession session = CqlSession
                .builder()
                .withKeyspace(ycqlContainer.getKeyspace())
                .withAuthCredentials(ycqlContainer.getUsername(), ycqlContainer.getPassword())
                .withLocalDatacenter(ycqlContainer.getLocalDc())
                .addContactPoint(ycqlContainer.getContactPoint())
                .build()
        ) {
            return session.execute(cql);
        }
    }
}
