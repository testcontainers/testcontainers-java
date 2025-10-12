package org.testcontainers.valkey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.valkey.JedisPool;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class ValkeyContainerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteAndReadEntry() {
        try (ValkeyContainer valkeyContainer = new ValkeyContainer()
            .withLogLevel(ValkeyLogLevel.DEBUG)
            .withSnapshotting(3, 1)) {

            valkeyContainer.start();
            JedisPool jedisPool = new JedisPool(valkeyContainer.createConnectionUrl());

            try (io.valkey.Jedis jedis = jedisPool.getResource()) {
                jedis.set("key", "value");
                assertThat(jedis.get("key")).isEqualTo("value");
            }
        }
    }

    @Test
    void shouldConfigureServiceWithAuthentication() {
        try (ValkeyContainer valkeyContainer = new ValkeyContainer()
            .withUsername("testuser")
            .withPassword("testpass")) {

            valkeyContainer.start();
            String url = valkeyContainer.createConnectionUrl();
            assertThat(url).contains("testuser:testpass");

            JedisPool jedisPool = new JedisPool(url);
            try (io.valkey.Jedis jedis = jedisPool.getResource()) {
                jedis.set("authKey", "authValue");
                assertThat(jedis.get("authKey")).isEqualTo("authValue");
            }
        }
    }

    @Test
    void shouldPersistData() {
        Path dataDir = tempDir.resolve("valkey-data");
        dataDir.toFile().mkdirs();

        try (ValkeyContainer valkeyContainer = new ValkeyContainer()
            .withPersistenceVolume(dataDir.toString())
            .withSnapshotting(1, 1)) {

            valkeyContainer.start();
            JedisPool jedisPool = new JedisPool(valkeyContainer.createConnectionUrl());

            try (io.valkey.Jedis jedis = jedisPool.getResource()) {
                jedis.set("persistKey", "persistValue");
            }

            valkeyContainer.stop();
            try (ValkeyContainer restarted = new ValkeyContainer()
                .withPersistenceVolume(dataDir.toString())) {
                restarted.start();
                JedisPool restartedPool = new JedisPool(restarted.createConnectionUrl());

                try (io.valkey.Jedis jedis = restartedPool.getResource()) {
                    assertThat(jedis.get("persistKey")).isEqualTo("persistValue");
                }
            }
        }
    }

    @Test
    void shouldValidateSnapshottingConfiguration() {
        ValkeyContainer container = new ValkeyContainer();
        assertThatThrownBy(() -> container.withSnapshotting(0, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("seconds must be greater than 0");

        assertThatThrownBy(() -> container.withSnapshotting(10, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("changedKeys must be non-negative");
    }

    @Test
    void shouldInitializeDatabaseWithInitialPayload() throws Exception {
        Path importFile = tempDir.resolve("import.data");
        String content = "SET key1 \"value1\"\nSET key2 \"value2\"";
        Files.write(importFile, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);

        try (ValkeyContainer valkeyContainer = new ValkeyContainer()
            .withInitialData(importFile.toString())) {

            valkeyContainer.start();
            JedisPool jedisPool = new JedisPool(valkeyContainer.createConnectionUrl());

            try (io.valkey.Jedis jedis = jedisPool.getResource()) {
                assertThat(jedis.get("key1")).isEqualTo("value1");
                assertThat(jedis.get("key2")).isEqualTo("value2");
            }
        }
    }
}
