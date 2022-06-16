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
 * Describes the different services that should be exposed on the container.
 */
public enum CouchbaseService {
    /**
     * Key-Value service.
     */
    KV("kv", 256),

    /**
     * Query (N1QL) service.
     * <p>
     * Note that the query service has no memory quota, so it is set to 0.
     */
    QUERY("n1ql", 0),

    /**
     * Search (FTS) service.
     */
    SEARCH("fts", 256),

    /**
     * Indexing service (needed if QUERY is also used!).
     */
    INDEX("index", 256),

    /**
     * Analytics service.
     */
    ANALYTICS("cbas", 1024),

    /**
     * Eventing service.
     */
    EVENTING("eventing", 256);

    private final String identifier;

    private final int minimumQuotaMb;

    CouchbaseService(final String identifier, final int minimumQuotaMb) {
        this.identifier = identifier;
        this.minimumQuotaMb = minimumQuotaMb;
    }

    /**
     * Returns the internal service identifier.
     *
     * @return the internal service identifier.
     */
    String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the minimum quota for the service in MB.
     *
     * @return the minimum quota in MB.
     */
    int getMinimumQuotaMb() {
        return minimumQuotaMb;
    }

    /**
     * Returns true if the service has a quota that needs to be applied.
     *
     * @return true if its quota needs to be applied.
     */
    boolean hasQuota() {
        return minimumQuotaMb > 0;
    }
}
