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

import java.util.ArrayList;
import java.util.List;

/**
 * Allows to configure a scope and its collections (available in Couchbase Server 7.0 and later).
 */
public class ScopeDefinition {

    private final String name;
    private final List<CollectionDefinition> collections;

    public ScopeDefinition(final String name) {
        this(name, new ArrayList<>());
    }

    public ScopeDefinition(final String name, final List<CollectionDefinition> collections) {
        this.name = name;
        this.collections = collections;
    }

    public ScopeDefinition withCollection(final String name) {
        collections.add(new CollectionDefinition(name));
        return this;
    }

    public String getName() {
        return name;
    }

    public List<CollectionDefinition> getCollections() {
        return collections;
    }
}
