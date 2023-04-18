package com.racing.fastestlap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public class FastestLapApplicationDemoApplication {

	static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.9"));

	@Autowired
	KafkaTemplate<String, LapTime> kafkaTemplate;

	public static void main(String[] args) {
		kafka.start();
		System.setProperty("spring.kafka.bootstrap-servers", kafka.getBootstrapServers());
		SpringApplication.run(FastestLapApplication.class, args);
	}

}
