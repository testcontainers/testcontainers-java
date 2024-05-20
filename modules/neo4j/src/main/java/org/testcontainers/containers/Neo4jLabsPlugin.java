package org.testcontainers.containers;

/**
 * Reflects a plugin from the official Neo4j 4.4.
 * <a href="https://neo4j.com/docs/operations-manual/4.4/docker/operations/#docker-neo4jlabs-plugins">Neo4j Labs Plugin list</a>.
 * There might be plugins not supported by your selected version of Neo4j.
 *
 * @deprecated Please use {@link Neo4jContainer#withLabsPlugins(String...)} with the matching plugin name for your Neo4j version.
 * Due to some renaming of the (Docker image) plugin names, there is no naming consistency across versions.
 * The plugins are listed here
 * <ul>
 *     <li><a href="https://neo4j.com/docs/operations-manual/5/configuration/plugins/">Neo4j 5</a></li>
 *     <li><a href="https://neo4j.com/docs/operations-manual/4.4/docker/operations/#docker-neo4jlabs-plugins">Neo4j 4.4</a></li>
 * </ul>
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
