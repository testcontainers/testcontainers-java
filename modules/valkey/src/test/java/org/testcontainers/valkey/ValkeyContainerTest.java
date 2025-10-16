package org.testcontainers.valkey;

import io.valkey.Jedis;
import io.valkey.JedisPool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValkeyContainerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteAndReadEntry() {
        try (
            ValkeyContainer valkeyContainer = new ValkeyContainer()
                .withLogLevel(ValkeyLogLevel.DEBUG)
                .withSnapshotting(3, 1)
        ) {
            valkeyContainer.start();
            try (JedisPool jedisPool = new JedisPool(valkeyContainer.createConnectionUrl());
                Jedis jedis = jedisPool.getResource()) {
                jedis.set("key", "value");
                assertThat(jedis.get("key")).isEqualTo("value");
            }
        }
    }

    @Test
    void shouldConfigureServiceWithAuthentication() {
        try (
            ValkeyContainer valkeyContainer = new ValkeyContainer().withUsername("testuser")
                .withPassword("testpass")
        ) {
            valkeyContainer.start();
            String url = valkeyContainer.createConnectionUrl();
            assertThat(url).contains("testuser:testpass");

            try (JedisPool jedisPool = new JedisPool(url);
                Jedis jedis = jedisPool.getResource()) {
                jedis.set("k1", "v2");
                assertThat(jedis.get("k1")).isEqualTo("v2");
            }
        }
    }


    @Test
    void shouldPersistData() {
        Path dataDir = tempDir.resolve("valkey-data");
        dataDir.toFile().mkdirs();

        try (
            ValkeyContainer valkeyContainer = new ValkeyContainer()
                .withPersistenceVolume(dataDir.toString())
                .withSnapshotting(1, 1)
        ) {
            valkeyContainer.start();

            String containerConnectionUrl = valkeyContainer.createConnectionUrl();
            try (JedisPool jedisPool = new JedisPool(containerConnectionUrl);
                Jedis jedis = jedisPool.getResource()) {
                jedis.set("persistKey", "persistValue");
            }

            valkeyContainer.stop();
            try (ValkeyContainer restarted = new ValkeyContainer().withPersistenceVolume(
                dataDir.toString())) {
                restarted.start();
                String connectionUrl = restarted.createConnectionUrl();

                try (JedisPool restartedPool = new JedisPool(connectionUrl);
                    Jedis jedis = restartedPool.getResource()) {
                    assertThat(jedis.get("persistKey")).isEqualTo("persistValue");
                }
            }
        }
    }

    @Test
    void shouldInitializeDatabaseWithPayload() throws Exception {
        Path importFile = Paths.get(getClass().getResource("/initData.valkey").toURI());

        try (ValkeyContainer valkeyContainer = new ValkeyContainer().withInitialData(
            importFile.toString())) {
            valkeyContainer.start();
            String connectionUrl = valkeyContainer.createConnectionUrl();

            try (JedisPool jedisPool = new JedisPool(
                connectionUrl); Jedis jedis = jedisPool.getResource()) {
                assertThat(jedis.get("key1")).isEqualTo("value1");
                assertThat(jedis.get("key2")).isEqualTo("value2");
            }
        }
    }

    @Test
    void shouldExecuteContainerCmdAndReturnResult() {
        try (ValkeyContainer valkeyContainer = new ValkeyContainer()) {
            valkeyContainer.start();

            String queryResult = valkeyContainer.executeCli("info", "clients");

            assertThat(queryResult).contains("connected_clients:1");
        }
    }

    @Test
    void shouldMountValkeyConfigToContainer() throws Exception {
        Path configFile = Paths.get(getClass().getResource("/valkey.conf").toURI());

        try (ValkeyContainer valkeyContainer = new ValkeyContainer().withConfigFile(
            configFile.toString())) {
            valkeyContainer.start();

            String connectionUrl = valkeyContainer.createConnectionUrl();
            try (JedisPool jedisPool = new JedisPool(connectionUrl);
                Jedis jedis = jedisPool.getResource()) {
                String maxMemory = jedis.configGet("maxmemory").get("maxmemory");

                assertThat(maxMemory).isEqualTo("2097152");
            }
        }
    }

    @Test
    void shouldValidateSnapshottingConfiguration() {
        try (ValkeyContainer container = new ValkeyContainer()) {
            assertThatThrownBy(() -> container.withSnapshotting(0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seconds must be greater than 0");

            assertThatThrownBy(() -> container.withSnapshotting(10, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("changedKeys must be non-negative");
        }
    }
}
