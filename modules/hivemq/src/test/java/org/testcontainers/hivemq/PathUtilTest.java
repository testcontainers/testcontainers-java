package org.testcontainers.hivemq;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathUtilTest {

    @Test
    void prepareInnerPath_emptyString() {
        assertThat(PathUtil.prepareInnerPath("")).isEqualTo("/");
    }

    @Test
    void prepareInnerPath_onlyDelimiter() {
        assertThat(PathUtil.prepareInnerPath("/")).isEqualTo("/");
    }

    @Test
    void prepareInnerPath_noDelimiter() {
        assertThat(PathUtil.prepareInnerPath("path")).isEqualTo("/path/");
    }

    @Test
    void prepareInnerPath_delimiterAtEnd() {
        assertThat(PathUtil.prepareInnerPath("path/")).isEqualTo("/path/");
    }

    @Test
    void prepareInnerPath_delimiterAtBeginning() {
        assertThat(PathUtil.prepareInnerPath("/path")).isEqualTo("/path/");
    }

    @Test
    void prepareAppendPath_delimiterAtBeginning_noChange() {
        assertThat(PathUtil.prepareAppendPath("/path")).isEqualTo("/path");
    }

    @Test
    void prepareAppendPath_delimiterAtBeginning_delimiterAdded() {
        assertThat(PathUtil.prepareAppendPath("path")).isEqualTo("/path");
    }
}
