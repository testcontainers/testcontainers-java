package org.testcontainers.containers;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.MySQLContainer.MYSQL_ALLOW_EMPTY_PASSWORD;
import static org.testcontainers.containers.MySQLContainer.MYSQL_ROOT_PASSWORD;
import static org.testcontainers.containers.MySQLContainer.MYSQL_USER;

public class MySQLContainerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldHaveProperDefaultRootUserSettings() {
        MySQLContainer<?> db = new MySQLContainer<>("mysql:8")
            .withUsername("testUser");
        db.configure();

        assertThat(db.getEnvMap().get(MYSQL_USER)).isEqualTo("testUser");
        assertThat(db.getEnvMap().get(MYSQL_ROOT_PASSWORD)).isEqualTo("test");
    }

    @Test
    public void shouldIgnoreCreatingUserWhenRoot() {
        MySQLContainer<?> db = new MySQLContainer<>("mysql:8")
            .withUsername("root")
            .withPassword("pass");
        db.configure();

        assertThat(db.getEnvMap().get(MYSQL_USER)).isNull();
        assertThat(db.getEnvMap().get(MYSQL_ROOT_PASSWORD)).isEqualTo("pass");
    }

    @Test
    public void shouldAllowEmptyPasswordWhenRoot() {
        MySQLContainer<?> db = new MySQLContainer<>("mysql:8")
            .withUsername("root")
            .withPassword("");
        db.configure();

        assertThat(db.getEnvMap().get(MYSQL_ALLOW_EMPTY_PASSWORD)).isEqualTo("yes");
    }

    @Test
    public void shouldThrowExceptionOnEmptyUserPassword() {
        MySQLContainer<?> db = new MySQLContainer<>("mysql:8")
            .withUsername("user")
            .withPassword("");

        thrown.expect(ContainerLaunchException.class);
        thrown.expectMessage("Empty password can be used only with the root user");

        db.configure();


    }
}
