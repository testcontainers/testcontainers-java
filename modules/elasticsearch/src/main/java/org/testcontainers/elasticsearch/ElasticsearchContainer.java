/*
 * Licensed to David Pilato (the "Author") under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Author licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.testcontainers.elasticsearch;

import org.apache.http.HttpHost;
import org.testcontainers.containers.GenericContainer;

/**
 * Represents an elasticsearch docker instance which exposes by default port 9200 and 9300 (transport.tcp.port)
 * The docker image is by default fetch from docker.elastic.co/elasticsearch/elasticsearch
 * @author dadoonet
 */
public class ElasticsearchContainer extends GenericContainer {

    /**
     * Elasticsearch Default HTTP port
     */
    private static final int ELASTICSEARCH_DEFAULT_PORT = 9200;

    /**
     * Elasticsearch Default Transport port
     */
    private static final int ELASTICSEARCH_DEFAULT_TCP_PORT = 9300;

    /**
     * Elasticsearch Docker base URL
     */
    private static final String ELASTICSEARCH_DEFAULT_BASE_URL = "docker.elastic.co/elasticsearch/elasticsearch";

    /**
     * Elasticsearch Default version
     */
    private static final String ELASTICSEARCH_DEFAULT_VERSION = "6.3.2";

    private String baseUrl = ELASTICSEARCH_DEFAULT_BASE_URL;
    private String version = ELASTICSEARCH_DEFAULT_VERSION;
    private String dockerImageName = null;

    public ElasticsearchContainer() {

    }

    /**
     * Create an Elasticsearch Container by passing the full docker image name
     * @param dockerImageName Full docker image name, like: docker.elastic.co/elasticsearch/elasticsearch:6.3.2
     */
    public ElasticsearchContainer(String dockerImageName) {
        this.dockerImageName = dockerImageName;
    }

    /**
     * Define the elasticsearch version to start
     * @param version  Elasticsearch Version like 5.6.10 or 6.3.2
     * @return this
     */
    public ElasticsearchContainer withVersion(String version) {
        this.version = version;
        return this;
    }

    /**
     * Define the elasticsearch docker registry base url
     * @param baseUrl  defaults to docker.elastic.co/elasticsearch/elasticsearch
     * @return this
     */
    public ElasticsearchContainer withBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    @Override
    protected void configure() {
        String image;
        if (dockerImageName == null) {
            image = baseUrl + ":" + version;
        } else {
            image = dockerImageName;
        }
        logger().info("Starting an elasticsearch container using [{}]", image);
        setDockerImageName(image);
        addExposedPorts(ELASTICSEARCH_DEFAULT_PORT, ELASTICSEARCH_DEFAULT_TCP_PORT);
    }

    public HttpHost getHost() {
        return new HttpHost(getContainerIpAddress(), getMappedPort(ELASTICSEARCH_DEFAULT_PORT));
    }
}
