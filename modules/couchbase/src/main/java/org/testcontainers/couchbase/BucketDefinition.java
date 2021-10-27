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
 * Allows to configure the properties of a bucket that should be created.
 */
public class BucketDefinition {

    private final String name;
    private boolean flushEnabled = false;
    private boolean queryPrimaryIndex = true;
    private int quota = 100;

    public BucketDefinition(final String name) {
        this.name = name;
    }

    /**
     * Enables flush for this bucket (disabled by default).
     *
     * @param flushEnabled if true, the bucket can be flushed.
     * @return this {@link BucketDefinition} for chaining purposes.
     */
    public BucketDefinition withFlushEnabled(final boolean flushEnabled) {
        this.flushEnabled = flushEnabled;
        return this;
    }

    /**
     * Sets a custom bucket quota (100MB by default).
     *
     * @param quota the quota to set for the bucket.
     * @return this {@link BucketDefinition} for chaining purposes.
     */
    public BucketDefinition withQuota(final int quota) {
        if (quota < 100) {
          throw new IllegalArgumentException("Bucket quota cannot be less than 100MB!");
        }
        this.quota = quota;
        return this;
    }

    /**
     * Allows to disable creating a primary index for this bucket (enabled by default).
     *
     * @param create if false, a primary index will not be created.
     * @return this {@link BucketDefinition} for chaining purposes.
     */
    public BucketDefinition withPrimaryIndex(final boolean create) {
        this.queryPrimaryIndex = create;
        return this;
    }

    public String getName() {
        return name;
    }

    public boolean hasFlushEnabled() {
        return flushEnabled;
    }

    public boolean hasPrimaryIndex() {
        return queryPrimaryIndex;
    }

    public int getQuota() {
        return quota;
    }

}
