package org.testcontainers.milvus;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import static org.assertj.core.api.Assertions.assertThat;

public class MilvusContainerTest {

    @Test
    public void withDefaultConfig() {
        try (
            // milvusContainer {
            MilvusContainer milvus = new MilvusContainer("milvusdb/milvus:v2.3.9")
            // }
        ) {
            milvus.start();

            assertThat(milvus.getEnvMap()).doesNotContainKey("ETCD_ENDPOINTS");
            assertMilvusVersion(milvus);
        }
    }

    @Test
    public void withExternalEtcd() {
        try (
            // externalEtcd {
            Network network = Network.newNetwork();
            GenericContainer<?> etcd = new GenericContainer<>("quay.io/coreos/etcd:v3.5.5")
                .withNetwork(network)
                .withNetworkAliases("etcd")
                .withCommand(
                    "etcd",
                    "-advertise-client-urls=http://127.0.0.1:2379",
                    "-listen-client-urls=http://0.0.0.0:2379",
                    "--data-dir=/etcd"
                )
                .withEnv("ETCD_AUTO_COMPACTION_MODE", "revision")
                .withEnv("ETCD_AUTO_COMPACTION_RETENTION", "1000")
                .withEnv("ETCD_QUOTA_BACKEND_BYTES", "4294967296")
                .withEnv("ETCD_SNAPSHOT_COUNT", "50000")
                .waitingFor(Wait.forLogMessage(".*ready to serve client requests.*", 1));
            MilvusContainer milvus = new MilvusContainer("milvusdb/milvus:v2.3.9")
                .withNetwork(network)
                .withEtcdEndpoint("etcd:2379")
                .dependsOn(etcd)
            // }
        ) {
            milvus.start();

            assertThat(milvus.getEnvMap()).doesNotContainKey("ETCD_USE_EMBED");
            assertMilvusVersion(milvus);
        }
    }

    private static void assertMilvusVersion(MilvusContainer milvus) {
        MilvusServiceClient milvusClient = new MilvusServiceClient(
            ConnectParam.newBuilder().withUri(milvus.getEndpoint()).build()
        );
        assertThat(milvusClient.getVersion().getData().getVersion()).isEqualTo("v2.3.9");
    }
}
