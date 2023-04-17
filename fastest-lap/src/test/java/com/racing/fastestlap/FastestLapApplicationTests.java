package com.racing.fastestlap;

import io.restassured.RestAssured;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FastestLapApplicationTests {

	static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.9"));

	@LocalServerPort
	private int port;

	@Autowired
	KafkaTemplate<String, LapTime> kafkaTemplate;

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
	void fastestTimeIsQueryable() {
		kafkaTemplate.send("laptime", new LapTime("Lewis Hamilton", "Silverstone", 17));
		kafkaTemplate.send("laptime", new LapTime("Lewis Hamilton", "Silverstone", 18));
		kafkaTemplate.send("laptime", new LapTime("Lewis Hamilton", "Silverstone", 20));
		kafkaTemplate.send("laptime", new LapTime("Lewis Hamilton", "Silverstone", 13));
		kafkaTemplate.send("laptime", new LapTime("Lewis Hamilton", "Silverstone", 10));

		Awaitility.await().untilAsserted(() -> RestAssured.given()
				.contentType("application/json")
				.get("/fastest-lap/Lewis_Hamilton/Silverstone")
				.then().statusCode(200)
				.body("time", equalTo(10)));
	}

}
