package org.testcontainers.containers;

import java.net.URL;

import lombok.Data;

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
