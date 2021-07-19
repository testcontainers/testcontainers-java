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

/**
 * Lists all the possible sample buckets that can be loaded.
 *
 * Sample buckets contain example data, views, and indexes for your experimentation.
 */
public enum SampleBucket {

    TRAVEL_SAMPLE("travel-sample", 31591),

    BEER_SAMPLE("beer-sample", 7303),

    GAMESIM_SAMPLE("gamesim-sample", 586);

    private final String name;
    private final long numLoadedDocs;

    SampleBucket(String name, long numLoadedDocs) {
        this.name = name;
        this.numLoadedDocs = numLoadedDocs;
    }

    String getName() {
        return name;
    }

    long getNumLoadedDocs() {
        return numLoadedDocs;
    }
}
