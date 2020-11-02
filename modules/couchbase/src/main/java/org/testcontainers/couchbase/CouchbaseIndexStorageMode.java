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
 * The storage mode to be used for all global secondary indexes in the cluster.
 * <p>
 * Please note: "plasma" and "memory optimized" are options in the Enterprise Edition of Couchbase Server. If you are
 * using the Community Edition, the only value allowed is forestdb.
 */
public enum CouchbaseIndexStorageMode {

    /**
     * A value of MEMORY_OPTIMIZED sets the cluster-wide index storage mode to use memory optimized global
     * secondary indexes which can perform index maintenance and index scan faster at in-memory speeds.
     * <p>
     * This is the default value for the testcontainers couchbase implementation.
     */
    MEMORY_OPTIMIZED {
        @Override
        public String toString() {
            return "memory_optimized";
        }
    },

    /**
     * A value of plasma sets the cluster-wide index storage mode to use the Plasma storage engine,
     * which can utilize both memory and persistent storage for index maintenance and index scans.
     */
    PLASMA {
        @Override
        public String toString() {
            return "plasma";
        }
    },

    /**
     * A value of forestdb sets the cluster-wide index storage mode to use the forestdb storage engine,
     * which only utilizes persistent storage for index maintenance and scans. It is the only option available
     * for the community edition.
     */
    FORESTDB {
        @Override
        public String toString() {
            return "forestdb";
        }
    };
}
