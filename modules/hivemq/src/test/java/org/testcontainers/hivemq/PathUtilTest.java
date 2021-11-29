/*
 * MIT License
 *
 * Copyright (c) 2021-present HiveMQ GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
