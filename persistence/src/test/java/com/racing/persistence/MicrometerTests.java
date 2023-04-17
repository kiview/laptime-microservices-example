package com.racing.persistence;

import io.restassured.RestAssured;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MicrometerTests {

	@LocalManagementPort
	private int localPort;

	static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.9"));
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.2"));

	@Autowired
	KafkaTemplate<String, LapTime> kafkaTemplate;

	@Autowired
	LapTimeRepository lapTimeRepository;

	@DynamicPropertySource
	static void kafkaProperties(DynamicPropertyRegistry registry) {
		Stream.of(kafka, postgres).parallel().forEach(GenericContainer::start);
		registry.add("spring.kafka.bootstrap-servers", () -> kafka.getBootstrapServers());
		registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
		registry.add("spring.datasource.username", () -> postgres.getUsername());
		registry.add("spring.datasource.password", () -> postgres.getPassword());
	}

	@Test
	void contextLoads() {
	}

	@Test
	void prometheusEndpointsIsAccessible() {
		RestAssured.port = localPort;

		RestAssured.get("/actuator/health").then().statusCode(200).log().all();
		RestAssured.get("/actuator/prometheus").then().statusCode(200).log().all();
	}

	@Test
	void containersCustomTimer() {
		kafkaTemplate.send("laptime", new LapTime("driver", "track", 1234567890));

		Awaitility.await().untilAsserted(() -> {
			assertThat(lapTimeRepository.count()).isEqualTo(1L);
		});

		RestAssured.port = localPort;
		String prometheusExporterResponse = RestAssured.get("/actuator/prometheus")
				.then().statusCode(200)
				.log().all()
				.extract().asString();

		assertThat(prometheusExporterResponse).contains("persistence_laptime_seconds");
	}

}
