package com.racing.persistence;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PersistenceApplicationTests {

	static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.9"));
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.2"));

	@LocalServerPort
	private int port;

	@Autowired
	KafkaTemplate<String, LapTime> kafkaTemplate;

	@Autowired
	LapTimeRepository lapTimeRepository;

	@DynamicPropertySource
	static void kafkaProperties(DynamicPropertyRegistry registry) {
		kafka.start();
		postgres.start();
		registry.add("spring.kafka.bootstrap-servers", () -> kafka.getBootstrapServers());
		registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
		registry.add("spring.datasource.username", () -> postgres.getUsername());
		registry.add("spring.datasource.password", () -> postgres.getPassword());
	}

	@Test
	void contextLoads() {
	}

	@Test
	void timesArePersisted() {
		var result = kafkaTemplate.send("laptime", new LapTime("driver", "track", 1234567890));
		result.whenComplete((r, e) -> {
			if (e != null) {
				e.printStackTrace();
			}
		});

		Awaitility.await().untilAsserted(() -> {
			Assertions.assertThat(lapTimeRepository.count()).isGreaterThan(0L);
		});

	}

}
