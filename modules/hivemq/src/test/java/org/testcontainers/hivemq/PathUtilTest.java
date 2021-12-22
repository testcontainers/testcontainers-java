package org.testcontainers.hivemq;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Yannick Weber
 */
class PathUtilTest {

    @Test
    void prepareInnerPath_emptyString() {
        assertEquals("/", PathUtil.prepareInnerPath(""));
    }

    @Test
    void prepareInnerPath_onlyDelimiter() {
        assertEquals("/", PathUtil.prepareInnerPath("/"));
    }

    @Test
    void prepareInnerPath_noDelimiter() {
        assertEquals("/path/", PathUtil.prepareInnerPath("path"));
    }

    @Test
    void prepareInnerPath_delimiterAtEnd() {
        assertEquals("/path/", PathUtil.prepareInnerPath("path/"));
    }

    @Test
    void prepareInnerPath_delimiterAtBeginning() {
        assertEquals("/path/", PathUtil.prepareInnerPath("/path"));
    }

    @Test
    void prepareAppendPath_delimiterAtBeginning_noChange() {
        assertEquals("/path", PathUtil.prepareAppendPath("/path"));
    }

    @Test
    void prepareAppendPath_delimiterAtBeginning_delimiterAdded() {
        assertEquals("/path", PathUtil.prepareAppendPath("path"));
    }
}
