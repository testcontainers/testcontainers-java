package org.testcontainers.containers;

import lombok.Data;

import java.net.URL;

/**
 * @author Simon Schneider
 */
@Data
public class SolrContainerConfiguration {

    private boolean zookeeper = true;

    private String collectionName = "dummy";

    private String configurationName;

    private URL solrConfiguration;

    private URL solrSchema;
}
