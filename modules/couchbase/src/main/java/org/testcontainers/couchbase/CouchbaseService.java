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
  KV("kv"),

  /**
   * Query (N1QL) service.
   */
  QUERY("n1ql"),

  /**
   * Search (FTS) service.
   */
  SEARCH("fts"),

  /**
   * Indexing service (needed if QUERY is also used!).
   */
  INDEX("index");

  private final String identifier;

  CouchbaseService(String identifier) {
    this.identifier = identifier;
  }

  String getIdentifier() {
    return identifier;
  }
}
