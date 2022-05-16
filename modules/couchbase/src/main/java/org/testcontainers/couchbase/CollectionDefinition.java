/*
 * Copyright (c) 2020 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testcontainers.couchbase;

/**
 * Allows to configure the properties of a collection that should be created.
 */
public class CollectionDefinition {

    private final String name;
    private int maxTTL = 0;

    public CollectionDefinition(final String name) {
        this.name = name;
    }

    /**
     * Max TTL for the collection. 0 Disables TTL for the collection.
     *
     * @param maxTTL the max TTL to set for the collection.
     * @return this {@link CollectionDefinition} for chaining purposes.
     */
    public CollectionDefinition withMaxTTL(int maxTTL) {
        this.maxTTL = maxTTL;
        return this;
    }

    public String getName() {
        return name;
    }

    public int getMaxTTL() {
        return maxTTL;
    }
}
