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

import org.jetbrains.annotations.NotNull;

/**
 * @author Yannick Weber
 */
class PathUtil {

    static @NotNull String prepareInnerPath(@NotNull String innerPath) {
        if ("/".equals(innerPath) || innerPath.isEmpty()) {
            return "/";
        }
        if (!innerPath.startsWith("/")) {
            innerPath = "/" + innerPath;
        }
        if (!innerPath.endsWith("/")) {
            innerPath += "/";
        }
        return innerPath;
    }

    static @NotNull String prepareAppendPath(@NotNull String appendPath) {
        if (!appendPath.startsWith("/")) {
            appendPath = "/" + appendPath;
        }
        return appendPath;
    }
}
