package org.testcontainers.containers;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.junit.After;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Simon Schneider
 */
public class JwksContainerTest {

    private static final DockerImageName SOLR_IMAGE = DockerImageName.parse("solr:8.3.0");

}
