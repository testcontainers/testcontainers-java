package org.testcontainers.cyrus;

import jakarta.mail.Folder;
import jakarta.mail.Session;
import jakarta.mail.Store;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CyrusContainerTest {

    @Test
    void shouldStartAndExposeAllEndpoints() throws Exception {
        try ( // container {
            CyrusContainer cyrus = new CyrusContainer(CyrusTestImages.CYRUS_IMAGE)
            // }
        ) {
            cyrus.start();

            assertThat(cyrus.getImapPort()).isEqualTo(cyrus.getMappedPort(8143));
            assertThat(cyrus.getPop3Port()).isEqualTo(cyrus.getMappedPort(8110));
            assertThat(cyrus.getHttpPort()).isEqualTo(cyrus.getMappedPort(8080));
            assertThat(cyrus.getLmtpPort()).isEqualTo(cyrus.getMappedPort(8024));
            assertThat(cyrus.getSievePort()).isEqualTo(cyrus.getMappedPort(4190));
            assertThat(cyrus.getManagementPort()).isEqualTo(cyrus.getMappedPort(8001));

            assertThat(cyrus.getImapUrl()).isEqualTo("imap://" + cyrus.getHost() + ":" + cyrus.getImapPort());
            assertThat(cyrus.getPop3Url()).isEqualTo("pop3://" + cyrus.getHost() + ":" + cyrus.getPop3Port());
            assertThat(cyrus.getHttpBaseUrl()).isEqualTo("http://" + cyrus.getHost() + ":" + cyrus.getHttpPort());
            assertThat(cyrus.getJmapUrl()).isEqualTo(cyrus.getHttpBaseUrl() + "/jmap/");
            assertThat(cyrus.getManagementUrl()).isEqualTo("http://" + cyrus.getHost() + ":" + cyrus.getManagementPort());

            assertThat(cyrus.getLivenessCheckPortNumbers())
                .containsExactlyInAnyOrder(
                    cyrus.getMappedPort(8143),
                    cyrus.getMappedPort(8110),
                    cyrus.getMappedPort(8080),
                    cyrus.getMappedPort(8024),
                    cyrus.getMappedPort(4190),
                    cyrus.getMappedPort(8001)
                );

            String managementRootResponse = get(cyrus.getManagementUrl() + "/");
            assertThat(managementRootResponse).contains("Basic test server");
        }
    }

    @Test
    void shouldSeedEmptyUserOnStartup() throws Exception {
        try ( // startupSeeding {
            CyrusContainer cyrus = new CyrusContainer(CyrusTestImages.CYRUS_IMAGE)
                .withSkipCreateUsers(true)
                .withSeedEmptyUser("alice")
            // }
        ) {
            cyrus.start();

            String exportedUser = cyrus.exportUser("alice");
            assertThat(exportedUser).contains("\"INBOX\"");
        }
    }

    @Test
    void shouldReplaceSeededUserDeterministically() throws Exception {
        CyrusUser firstDeclaration = CyrusUser
            .builder("seeded")
            .withoutDefaultMailboxes()
            .addMailbox("INBOX")
            .addMailbox("FirstOnly")
            .build();
        CyrusUser lastDeclaration = CyrusUser
            .builder("seeded")
            .withoutDefaultMailboxes()
            .addMailbox("INBOX")
            .addMailbox("SecondOnly")
            .build();

        try (
            CyrusContainer cyrus = new CyrusContainer(CyrusTestImages.CYRUS_IMAGE)
                .withSkipCreateUsers(true)
                .withSeedUser(firstDeclaration)
                .withSeedUser(lastDeclaration)
        ) {
            cyrus.start();

            String exportedUser = cyrus.exportUser("seeded");
            assertThat(exportedUser).contains("SecondOnly");
            assertThat(exportedUser).doesNotContain("FirstOnly");
        }
    }

    @Test
    void shouldSupportIdempotentOperations() throws Exception {
        try (CyrusContainer cyrus = new CyrusContainer(CyrusTestImages.CYRUS_IMAGE).withSkipCreateUsers(true)) {
            cyrus.start();

            CyrusUser idempotentUser = CyrusUser.builder("idempotent").build();

            assertThat(cyrus.userExists("idempotent")).isFalse();
            assertThat(cyrus.createUserIfMissing(idempotentUser)).isTrue();
            assertThat(cyrus.createUserIfMissing(idempotentUser)).isFalse();
            assertThat(cyrus.userExists("idempotent")).isTrue();

            Optional<String> exportedUser = cyrus.exportUserIfExists("idempotent");
            assertThat(exportedUser).isPresent();
            assertThat(exportedUser.get()).contains("\"INBOX\"");

            assertThat(cyrus.deleteUserIfExists("idempotent")).isTrue();
            assertThat(cyrus.deleteUserIfExists("idempotent")).isFalse();
            assertThat(cyrus.exportUserIfExists("idempotent")).isEmpty();
        }
    }

    @Test
    void shouldKeepStrictOperationsFailFast() throws Exception {
        try (CyrusContainer cyrus = new CyrusContainer(CyrusTestImages.CYRUS_IMAGE).withSkipCreateUsers(true)) {
            cyrus.start();

            assertThatThrownBy(() -> cyrus.exportUser("missing-user"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("HTTP 404");
            assertThatThrownBy(() -> cyrus.deleteUser("missing-user"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("HTTP 404");
        }
    }

    @Test
    void shouldRemoveDefaultUsersWhenSkipCreateUsersEnabled() throws Exception {
        try (CyrusContainer cyrus = new CyrusContainer(CyrusTestImages.CYRUS_IMAGE).withSkipCreateUsers(true)) {
            cyrus.start();

            assertThat(cyrus.userExists("user1")).isFalse();
            assertThatThrownBy(() -> cyrus.exportUser("user1")).isInstanceOf(IOException.class).hasMessageContaining("HTTP 404");
        }
    }

    @Test
    void shouldAuthenticateViaImapAfterUserImport() throws Exception {
        try (CyrusContainer cyrus = new CyrusContainer(CyrusTestImages.CYRUS_IMAGE).withSkipCreateUsers(true)) {
            cyrus.start();
            cyrus.upsertUser(CyrusUser.builder("imap-user").build());

            Properties properties = new Properties();
            properties.setProperty("mail.store.protocol", "imap");
            properties.setProperty("mail.imap.host", cyrus.getHost());
            properties.setProperty("mail.imap.port", String.valueOf(cyrus.getImapPort()));
            properties.setProperty("mail.imap.ssl.enable", "false");
            properties.setProperty("mail.imap.starttls.enable", "false");

            Session session = Session.getInstance(properties);
            Store store = session.getStore("imap");

            try {
                store.connect("imap-user", "x");
                Folder inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_ONLY);

                assertThat(inbox.exists()).isTrue();
                assertThat(inbox.getMessageCount()).isZero();

                inbox.close(false);
            } finally {
                store.close();
            }
        }
    }

    @Test
    void shouldApplyEnvironmentSetters() throws Exception {
        try (
            CyrusContainer cyrus = new CyrusContainer(CyrusTestImages.CYRUS_IMAGE)
                .withDefaultDomain("testcontainers.org")
                .withServerName("mail.testcontainers.org")
                .withRelayHost("smtp.fastmail.com")
                .withRelayAuth("user:pass")
                .withSkipCreateUsers(true)
        ) {
            assertThat(cyrus.getEnvMap()).containsEntry("SKIP_CREATE_USERS", "1");
            cyrus.withSkipCreateUsers(false);
            assertThat(cyrus.getEnvMap()).doesNotContainKey("SKIP_CREATE_USERS");
            cyrus.withSkipCreateUsers(true);

            cyrus.start();

            assertThat(cyrus.getEnvMap())
                .containsEntry("DEFAULTDOMAIN", "testcontainers.org")
                .containsEntry("SERVERNAME", "mail.testcontainers.org")
                .containsEntry("RELAYHOST", "smtp.fastmail.com")
                .containsEntry("RELAYAUTH", "user:pass")
                .containsEntry("SKIP_CREATE_USERS", "1");

            String configOutput = cyrus
                .execInContainer("sh", "-c", "grep -E '^(defaultdomain|servername):' /etc/imapd.conf")
                .getStdout();
            assertThat(configOutput)
                .contains("defaultdomain: testcontainers.org")
                .contains("servername: mail.testcontainers.org");
        }
    }

    @Test
    void shouldSetRefreshAndCyrusVersionVariables() {
        CyrusContainer cyrus = new CyrusContainer(CyrusTestImages.CYRUS_IMAGE).withRefresh(true).withCyrusVersion("main");

        assertThat(cyrus.getEnvMap()).containsEntry("REFRESH", "1").containsEntry("CYRUS_VERSION", "main");

        cyrus.withRefresh(false);
        assertThat(cyrus.getEnvMap()).doesNotContainKey("REFRESH").containsEntry("CYRUS_VERSION", "main");
    }

    private static String get(String targetUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(targetUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);

        try {
            int statusCode = connection.getResponseCode();
            String responseBody = read(connection.getInputStream());
            assertThat(statusCode).isEqualTo(HttpURLConnection.HTTP_OK);
            return responseBody;
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        try (InputStream in = inputStream) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                output.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            }
            return output.toString();
        }
    }
}
