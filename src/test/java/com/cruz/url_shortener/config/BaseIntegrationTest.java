package com.cruz.url_shortener.config;

import com.redis.testcontainers.RedisContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.postgresql.PostgreSQLContainer;

public abstract class BaseIntegrationTest {
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    static RedisContainer redis = new RedisContainer("redis:alpine");
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.jpa.hibernate.ddl-auto", ()-> "create-drop");
    }
    static{
        postgres.start();
        redis.start();
    }



}
