/*
 * Copyright (c) 2021 Couchbase, Inc.
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

public class CollectionDefinition {

    private final String name;
    private boolean queryPrimaryIndex = true;

    public CollectionDefinition(final String name) {
        this.name = name;
    }

    /**
     * Allows to disable creating a primary index for this collection (enabled by default).
     *
     * @param create if false, a primary index will not be created.
     * @return this {@link CollectionDefinition} for chaining purposes.
     */
    public CollectionDefinition withPrimaryIndex(final boolean create) {
        this.queryPrimaryIndex = create;
        return this;
    }

    public String getName() {
        return name;
    }

    public boolean hasPrimaryIndex() {
        return queryPrimaryIndex;
    }
}
