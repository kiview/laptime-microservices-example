package com.racing.ingestion;

import io.restassured.RestAssured;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IngestionApplicationTests {

	static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.9"));

	@LocalServerPort
	private int port;

	@BeforeEach
	void setupRestAssured() {
		RestAssured.port = port;
	}

	@DynamicPropertySource
	static void kafkaProperties(DynamicPropertyRegistry registry) {
		kafka.start();
		registry.add("spring.kafka.bootstrap-servers", () -> kafka.getBootstrapServers());
	}

	@Test
	void contextLoads() {
	}

	@Test
	void requestsAreSendToKafkaTopic() {
		var exampleLapTime = new LapTime("driver", "track", 1234567890);
		RestAssured.given()
				.contentType("application/json")
				.body(exampleLapTime)
				.post("/laptime")
				.then().statusCode(200);

		var kafkaConfig = new HashMap<String, Object>();
		kafkaConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		kafkaConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "tc-" + UUID.randomUUID());
		kafkaConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		kafkaConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
				StringDeserializer.class.getName());
		kafkaConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
				StringDeserializer.class.getName());

		try (var consumer = new KafkaConsumer<String, String>(kafkaConfig)) {
			consumer.subscribe(List.of("laptime"));

			Awaitility.await().untilAsserted(() -> {
				var records = consumer.poll(Duration.ofSeconds(5));
				assertFalse(records.isEmpty());
				Assertions.assertThat(records.iterator().next().value())
						.contains("driver", "track", "1234567890");
			});
		}

	}

}
