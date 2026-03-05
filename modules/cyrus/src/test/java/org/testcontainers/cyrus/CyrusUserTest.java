package org.testcontainers.cyrus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CyrusUserTest {

    private static final String DEFAULT_EMPTY_USER_JSON =
        "{\"mailboxes\":[{\"name\":\"INBOX\",\"subscribed\":true}," +
        "{\"name\":\"Archive\",\"subscribed\":true,\"specialUse\":\"\\\\Archive\"}," +
        "{\"name\":\"Drafts\",\"subscribed\":true,\"specialUse\":\"\\\\Drafts\"}," +
        "{\"name\":\"Sent\",\"subscribed\":true,\"specialUse\":\"\\\\Sent\"}," +
        "{\"name\":\"Spam\",\"subscribed\":true,\"specialUse\":\"\\\\Junk\"}," +
        "{\"name\":\"Trash\",\"subscribed\":true,\"specialUse\":\"\\\\Trash\"}]}";

    @Test
    void builderShouldDefaultToEmptyUserMailboxes() {
        // userBuilder {
        CyrusUser user = CyrusUser.builder("alice").build();
        // }

        assertThat(user.getUserId()).isEqualTo("alice");
        assertThat(user.toJson()).isEqualTo(DEFAULT_EMPTY_USER_JSON);
    }

    @Test
    void builderShouldAllowCustomMailboxList() {
        CyrusUser user = CyrusUser
            .builder("bob")
            .withoutDefaultMailboxes()
            .addMailbox("INBOX")
            .addMailbox("Projects", false, "\\Archive")
            .build();

        assertThat(user.toJson()).isEqualTo(
            "{\"mailboxes\":[{\"name\":\"INBOX\",\"subscribed\":true}," +
            "{\"name\":\"Projects\",\"subscribed\":false,\"specialUse\":\"\\\\Archive\"}]}"
        );
    }

    @Test
    void builderShouldValidateUserId() {
        assertThatThrownBy(() -> CyrusUser.builder(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CyrusUser.builder("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void builderShouldProduceDeterministicJsonOrder() {
        CyrusUser first = CyrusUser
            .builder("order")
            .withoutDefaultMailboxes()
            .addMailbox("INBOX")
            .addMailbox("A")
            .addMailbox("B")
            .build();
        CyrusUser second = CyrusUser
            .builder("order")
            .withoutDefaultMailboxes()
            .addMailbox("INBOX")
            .addMailbox("A")
            .addMailbox("B")
            .build();

        assertThat(first.toJson()).isEqualTo(second.toJson());
        assertThat(first.toJson().indexOf("\"A\"")).isLessThan(first.toJson().indexOf("\"B\""));
    }
}
