package org.testcontainers.cyrus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a Cyrus user payload that can be imported through the management API.
 */
public final class CyrusUser {

    private final String userId;

    private final List<Mailbox> mailboxes;

    private CyrusUser(String userId, List<Mailbox> mailboxes) {
        this.userId = userId;
        this.mailboxes = Collections.unmodifiableList(new ArrayList<Mailbox>(mailboxes));
    }

    public static Builder builder(String userId) {
        return new Builder(normalizeUserId(userId));
    }

    public String getUserId() {
        return userId;
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\"mailboxes\":[");

        for (int i = 0; i < mailboxes.size(); i++) {
            Mailbox mailbox = mailboxes.get(i);
            if (i > 0) {
                json.append(',');
            }

            json.append("{\"name\":\"")
                .append(escapeJson(mailbox.name))
                .append("\",\"subscribed\":")
                .append(mailbox.subscribed);

            if (mailbox.specialUse != null) {
                json.append(",\"specialUse\":\"")
                    .append(escapeJson(mailbox.specialUse))
                    .append('"');
            }

            json.append('}');
        }

        json.append("]}");
        return json.toString();
    }

    public static final class Builder {

        private final String userId;

        private final List<Mailbox> mailboxes = new ArrayList<Mailbox>();

        private Builder(String userId) {
            this.userId = userId;
            withDefaultMailboxes();
        }

        public Builder withDefaultMailboxes() {
            mailboxes.clear();
            mailboxes.add(new Mailbox("INBOX", true, null));
            mailboxes.add(new Mailbox("Archive", true, "\\Archive"));
            mailboxes.add(new Mailbox("Drafts", true, "\\Drafts"));
            mailboxes.add(new Mailbox("Sent", true, "\\Sent"));
            mailboxes.add(new Mailbox("Spam", true, "\\Junk"));
            mailboxes.add(new Mailbox("Trash", true, "\\Trash"));
            return this;
        }

        public Builder withoutDefaultMailboxes() {
            mailboxes.clear();
            return this;
        }

        public Builder addMailbox(String name) {
            return addMailbox(name, true, null);
        }

        public Builder addMailbox(String name, boolean subscribed, String specialUse) {
            mailboxes.add(new Mailbox(normalizeMailboxName(name), subscribed, normalizeSpecialUse(specialUse)));
            return this;
        }

        public CyrusUser build() {
            return new CyrusUser(userId, mailboxes);
        }
    }

    private static final class Mailbox {

        private final String name;

        private final boolean subscribed;

        private final String specialUse;

        private Mailbox(String name, boolean subscribed, String specialUse) {
            this.name = name;
            this.subscribed = subscribed;
            this.specialUse = specialUse;
        }
    }

    private static String normalizeUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId must not be null or blank");
        }
        return userId.trim();
    }

    private static String normalizeMailboxName(String mailboxName) {
        if (mailboxName == null || mailboxName.trim().isEmpty()) {
            throw new IllegalArgumentException("mailbox name must not be null or blank");
        }
        return mailboxName.trim();
    }

    private static String normalizeSpecialUse(String specialUse) {
        if (specialUse == null) {
            return null;
        }
        String trimmed = specialUse.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("specialUse must not be blank");
        }
        return trimmed;
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }
        return escaped.toString();
    }
}
