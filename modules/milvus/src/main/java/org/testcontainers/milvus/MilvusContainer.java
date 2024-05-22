package org.testcontainers.milvus;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Testcontainers implementation for Milvus.
 * <p>
 * Supported image: {@code milvusdb/milvus}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Management port: 9091</li>
 *     <li>HTTP: 19530</li>
 * </ul>
 */
public class MilvusContainer extends GenericContainer<MilvusContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("milvusdb/milvus");

    private String etcdEndpoint;

    public MilvusContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public MilvusContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(9091, 19530);
        waitingFor(Wait.forHttp("/healthz").forPort(9091));
        withCommand("milvus", "run", "standalone");
        withCopyFileToContainer(
            MountableFile.forClasspathResource("testcontainers/embedEtcd.yaml"),
            "/milvus/configs/embedEtcd.yaml"
        );
        withEnv("COMMON_STORAGETYPE", "local");
    }

    @Override
    protected void configure() {
        if (this.etcdEndpoint == null) {
            withEnv("ETCD_USE_EMBED", "true");
            withEnv("ETCD_DATA_DIR", "/var/lib/milvus/etcd");
            withEnv("ETCD_CONFIG_PATH", "/milvus/configs/embedEtcd.yaml");
        } else {
            withEnv("ETCD_ENDPOINTS", this.etcdEndpoint);
        }
    }

    public MilvusContainer withEtcdEndpoint(String etcdEndpoint) {
        this.etcdEndpoint = etcdEndpoint;
        return this;
    }

    public String getEndpoint() {
        return "http://" + getHost() + ":" + getMappedPort(19530);
    }
}
