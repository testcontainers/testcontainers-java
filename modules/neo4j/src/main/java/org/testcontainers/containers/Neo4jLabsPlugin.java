package org.testcontainers.containers;

/**
 * Reflects a plugin from the official Neo4j 4.4.
 * <a href="https://neo4j.com/docs/operations-manual/4.4/docker/operations/#docker-neo4jlabs-plugins">Neo4j Labs Plugin list</a>.
 * There might be plugins not supported by your selected version of Neo4j.
 */
public enum Neo4jLabsPlugin {
    APOC("apoc"),
    APOC_CORE("apoc-core"),
    BLOOM("bloom"),
    STREAMS("streams"),
    GRAPH_DATA_SCIENCE("graph-data-science"),
    NEO_SEMANTICS("n10s");

    final String pluginName;

    Neo4jLabsPlugin(String pluginName) {
        this.pluginName = pluginName;
    }
}
