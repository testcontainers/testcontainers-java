package com.example;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestContainerConfiguration {

    private final static MySQLContainer<?> mySQLContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.30"))
        .withDatabaseName("databasename")
        .withUsername("user")
        .withPassword("password")
        .withInitScript("schema-test.sql")
        .withReuse(true);

    static {
        mySQLContainer.start();
    }

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
            .driverClassName(mySQLContainer.getDriverClassName())
            .username(mySQLContainer.getUsername())
            .password(mySQLContainer.getPassword())
            .url(mySQLContainer.getJdbcUrl())
            .type(HikariDataSource.class)
            .build();
    }
}
