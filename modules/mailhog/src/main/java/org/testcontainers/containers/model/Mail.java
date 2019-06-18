package org.testcontainers.containers.model;

import static org.testcontainers.containers.MailHogContainer.*;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * This model class represents an email.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode
@ToString
public class Mail {

    @JsonProperty("ID")
    private String id;

    @JsonProperty("Content")
    private Content content;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @EqualsAndHashCode
    @ToString
    public static class Content {
        @JsonProperty("Headers")
        private Map<String, List<String>> headers;
        @JsonProperty("Body")
        private String body;
    }

    /**
     * Returns the header with the name "Date".
     * @return the date
     */
    public ZonedDateTime getDate() {
        List<String> date = content.getHeaders().get(MAIL_HEADER_DATE);
        return ZonedDateTime.parse(date.get(0).replaceFirst(" \\(.+\\)$", StringUtils.EMPTY), DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    /**
     * Returns the subject
     * @return the subject
     */
    public String getSubject() {
        List<String> subjectList = content.getHeaders().get(MAIL_HEADER_SUBJECT);
        if (subjectList != null && !subjectList.isEmpty()) {
            return subjectList.get(0);
        } else {
            return StringUtils.EMPTY;
        }
    }

    /**
     * Returns the receivers of the mail (header name To)
     * @return the receivers
     */
    public List<String> getTo() {
        return getHeader(MAIL_HEADER_TO);
    }

    /**
     * Returns the cc receivers of the mail (header name Cc)
     * @return the cc receivers
     */
    public List<String> getCC() {
        return getHeader(MAIL_HEADER_CC);
    }

    /**
     * Returns the header with given header name. If there are multiple entries with the same header name (i.e.
     * Received), each entry will result in an entry of the list. In case of multiple entries in one header name (i.e.
     * To or Cc) separated by comma, the entry will be split using the separator "," and trimmed afterwards.
     * @param headerName
     * @return the list of headers
     */
    public List<String> getHeader(String headerName) {
        List<String> header = content.getHeaders().get(headerName);
        if (header != null && header.size() == 1) {
            return Arrays.stream(header.get(0).split(","))
                .map(StringUtils::trim)
                .collect(Collectors.toList());
        } else if(header != null && header.size() > 1) {
            return header;
        } else {
            return new ArrayList<>();
        }
    }
}
