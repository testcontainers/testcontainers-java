package com.example;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.GenericContainer;

import javax.annotation.PostConstruct;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(DemoApplicationTest.TestConfiguration.class)
@WebIntegrationTest(randomPort = true)
public abstract class AbstractIntegrationTest {

    @ClassRule
    public static GenericContainer redis = new GenericContainer("redis:3.0.6").withExposedPorts(6379);

    @Configuration
    @Import(DemoApplication.class)
    static class TestConfiguration {

        @Autowired
        ConfigurableEnvironment environment;

        @PostConstruct
        public void init() {
            environment.getPropertySources().addFirst(new PropertySource("containers") {
                @Override
                public Object getProperty(String name) {
                    switch (name) {
                        case "spring.redis.host":
                            return redis.getContainerIpAddress();
                        case "spring.redis.port":
                            return redis.getMappedPort(6379);
                        default:
                            return null;
                    }
                }
            });
        }
    }

    @Value("${local.server.port}")
    protected int port;
}
