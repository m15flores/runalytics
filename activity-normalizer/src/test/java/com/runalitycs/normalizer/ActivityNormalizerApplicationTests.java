package com.runalitycs.normalizer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
class ActivityNormalizerApplicationTests {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
			DockerImageName.parse("postgres:15-alpine")
	)
			.withDatabaseName("runalytics")
			.withUsername("runalytics")
			.withPassword("runalytics");

	@Container
	static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.7.0")
	);

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		// PostgreSQL
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);

		// Kafka
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

		// Hibernate: create schema automatically for tests
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
	}

	@Test
	void contextLoads() {
	}

}
