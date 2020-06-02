package org.testcontainers.containers;

import java.net.URL;
import java.util.HashMap;

import lombok.Data;

/**
 * @author Simon Schneider
 */
@Data
public class SolrContainerConfiguration {

    private boolean zookeeper = true;
    private String collectionName = "dummy";
    private String configurationName;
    private String schemaName;
    private HashMap<String, URL> resources = new HashMap<>();

    public SolrContainerConfiguration addResource(String name, URL resource) {
        resources.put(name, resource);
        return this;
    }

    public URL getSolrSchema() {
        return resources.get(schemaName);
    }

    public URL getSolrConfiguration() {
        return resources.get(configurationName);
    }
}
