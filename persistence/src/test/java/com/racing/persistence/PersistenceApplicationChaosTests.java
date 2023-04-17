package com.racing.persistence;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.*;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;

@SpringBootTest
class PersistenceApplicationChaosTests {

	static Network network = Network.newNetwork();
	static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.9"));
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.2"))
			.withNetwork(network)
			.withNetworkAliases("postgres");
	static ToxiproxyContainer toxiproxy = new ToxiproxyContainer(DockerImageName.parse("ghcr.io/shopify/toxiproxy:2.5.0"))
			.withNetwork(network);

	static Proxy proxy;

	@Autowired
	KafkaTemplate<String, LapTime> kafkaTemplate;

	@Autowired
	LapTimeRepository lapTimeRepository;

	@DynamicPropertySource
	static void kafkaProperties(DynamicPropertyRegistry registry) throws IOException {
		Stream.of(kafka, postgres, toxiproxy).parallel().forEach(GenericContainer::start);

		final ToxiproxyClient toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
		proxy = toxiproxyClient.createProxy("postgres", "0.0.0.0:8666", "postgres:5432");

		String proxiedPostgres = "jdbc:postgresql://" +
				toxiproxy.getHost() +
				":" +
				toxiproxy.getMappedPort(8666) +
				"/" +
				postgres.getDatabaseName();

		registry.add("spring.kafka.bootstrap-servers", () -> kafka.getBootstrapServers());
		registry.add("spring.datasource.url", () -> proxiedPostgres);
		registry.add("spring.datasource.username", () -> postgres.getUsername());
		registry.add("spring.datasource.password", () -> postgres.getPassword());
	}

	@Test
	void contextLoads() {
	}

	@Test
	void timesArePersisted() throws IOException, InterruptedException {

		kafkaTemplate.send("laptime", new LapTime("driver", "track", 1234567890));

		await().untilAsserted(() -> {
			Assertions.assertThat(lapTimeRepository.count()).isEqualTo(1L);
		});

		proxy.toxics().timeout("CUT_CONNECTION_DOWNSTREAM", ToxicDirection.DOWNSTREAM, 1);
		proxy.toxics().timeout("CUT_CONNECTION_UPSTREAM", ToxicDirection.UPSTREAM, 1);

		kafkaTemplate.send("laptime", new LapTime("driver", "track", 1234567890));

		// Would be better to query the database directly for some seconds to check no new data is written,
		// but for a demo it is ok. Sleep to make sure save is called with conection cut.
		Thread.sleep(3000);

		proxy.toxics().get("CUT_CONNECTION_DOWNSTREAM").remove();
		proxy.toxics().get("CUT_CONNECTION_UPSTREAM").remove();


		// Hibernate will eventually persist into the database after the connection is restored.
		await().untilAsserted(() -> {
			Assertions.assertThat(lapTimeRepository.count()).isEqualTo(2L);
		});

	}

}
