package org.testcontainers.containers;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.driver.ser.GryoMessageSerializerV3d0;
import org.apache.tinkerpop.gremlin.orientdb.io.OrientIoRegistry;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoMapper;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test uses a preconfigured image of OrientDB 3.0..x with the gremlin server that expose the "demodb" on 8182 port.
 *
 * @author robfrank
 */
public class OrientDBGremlinTest {


    @ClassRule
    public static OrientDBContainer container = new OrientDBContainer("arcadeanalytics/orientdb:3.0.18-tp3")
        .withExposedPorts(2424, 8182);


    @Test
    public void shouldQuerywithGremlin() {
        final Cluster cluster = Cluster.build(container.getContainerIpAddress())
            .port(container.getMappedPort(8182))
            .serializer(new GryoMessageSerializerV3d0(GryoMapper.build()
                .addRegistry(OrientIoRegistry.getInstance())))
            //the gremlin server uses the OrientDB server administrator
            .credentials("root", "root")
            .maxWaitForConnection(20000)
            .create();


        Client client = cluster.connect().init();

        final ResultSet resultSet = client.submit("g.V().hasLabel('Countries').has('Name', 'Italy')");

        assertThat(resultSet.one().getVertex().property("Name").value()).isEqualTo("Italy");

    }
}
