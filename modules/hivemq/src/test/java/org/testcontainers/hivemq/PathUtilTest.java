/*
 * Copyright 2020 HiveMQ and the HiveMQ Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
