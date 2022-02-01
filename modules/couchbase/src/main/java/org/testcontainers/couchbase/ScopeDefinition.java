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

import java.util.ArrayList;
import java.util.List;

/**
 * Allows to configure the properties of a scope that should be created.
 */
public class ScopeDefinition {

    private final String name;
    private List<CollectionDefinition> collections = new ArrayList<>();

    public ScopeDefinition(final String name) {
        this.name = name;
    }

    /**
     * Add a collection to this scope.
     *
     * @param collection, the collection definition.
     * @return this {@link ScopeDefinition} for chaining purposes.
     */
    public ScopeDefinition withCollection(CollectionDefinition collection) {
        this.collections.add(collection);
        return this;
    }

    public String getName() {
        return name;
    }

    public List<CollectionDefinition> getCollections() {
        return collections;
    }
}
