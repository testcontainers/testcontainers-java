package com.example.linkedcontainer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.Assert.assertEquals;

/**
 * Tests for RedmineClient.
 */
public class RedmineClientTest {

    private static final String POSTGRES_USERNAME = "redmine";
    private static final String POSTGRES_PASSWORD = "secret";

    private PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(LinkedContainerTestImages.POSTGRES_TEST_IMAGE)
            .withUsername(POSTGRES_USERNAME)
            .withPassword(POSTGRES_PASSWORD);

    private RedmineContainer redmineContainer = new RedmineContainer(LinkedContainerTestImages.REDMINE_TEST_IMAGE)
            .withLinkToContainer(postgreSQLContainer, "postgres")
            .withEnv("POSTGRES_ENV_POSTGRES_USER", POSTGRES_USERNAME)
            .withEnv("POSTGRES_ENV_POSTGRES_PASSWORD", POSTGRES_PASSWORD);

    @Rule
    public RuleChain chain = RuleChain.outerRule(postgreSQLContainer)
            .around(redmineContainer);

    @Test
    public void canGetIssueCount() throws Exception {
        RedmineClient redmineClient = new RedmineClient(
                redmineContainer.getRedmineUrl());

        assertEquals(
                "The issue count can be retrieved.",
                0,
                redmineClient.getIssueCount());
    }
}
