package org.testcontainers.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class NacosContainerTest {

    private ConfigService configService;

    private static NacosContainer nacos = new NacosContainer("nacos/nacos-server:v3.0.3");

    @BeforeAll
    static void setup() {
        nacos.start();
    }

    @AfterAll
    static void teardown() {
        nacos.stop();
    }

    @BeforeEach
    void init() throws NacosException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, nacos.getServerAddr());
        properties.put(PropertyKeyConst.USERNAME, "nacos");
        properties.put(PropertyKeyConst.PASSWORD, "nacos");
        configService = NacosFactory.createConfigService(properties);
    }


    @Test
    void writeAndRemoveValue() throws NacosException, InterruptedException {
        assertThat(configService.publishConfig("test.yaml", "DEFAULT", "name: 123")).isTrue();
        Thread.sleep(1500);
        assertThat(configService.getConfig("test.yaml", "DEFAULT", 5000)).isEqualTo("name: 123");
        assertThat(configService.removeConfig("test.yaml", "DEFAULT")).isTrue();
        Thread.sleep(1500);
        assertThat(configService.getConfig("test.yaml", "DEFAULT", 5000)).isEqualTo(null);
    }

}
