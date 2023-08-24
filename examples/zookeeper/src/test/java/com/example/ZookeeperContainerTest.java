package com.example;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ZookeeperContainerTest {

    private static final int ZOOKEEPER_PORT = 2181;

    @Test
    void test() throws Exception {
        String path = "/messages/zk-tc";
        String content = "Running Zookeeper with Testcontainers";
        try (
            GenericContainer<?> zookeeper = new GenericContainer<>("zookeeper:3.8.0").withExposedPorts(ZOOKEEPER_PORT)
        ) {
            zookeeper.start();

            String connectionString = zookeeper.getHost() + ":" + zookeeper.getMappedPort(ZOOKEEPER_PORT);
            CuratorFramework curatorFramework = CuratorFrameworkFactory
                .builder()
                .connectString(connectionString)
                .retryPolicy(new RetryOneTime(100))
                .build();
            curatorFramework.start();
            curatorFramework.create().creatingParentsIfNeeded().forPath(path, content.getBytes());

            byte[] bytes = curatorFramework.getData().forPath(path);
            curatorFramework.close();

            assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo(content);
        }
    }
}
