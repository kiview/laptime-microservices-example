package com.racing.fastestlap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Properties;

import static org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

public class FastestLapProcessorTest {

    private FastestLapProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new FastestLapProcessor();
    }

    @Test
    void shouldReturnFastestLap() {
        var streamBuilder = new StreamsBuilder();
        processor.buildFastestLapPipeline(streamBuilder, new ObjectMapper());
        var topology = streamBuilder.build();

        var props = new Properties();
        props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        try (var testDriver = new TopologyTestDriver(topology, props)) {
            var inputTopic = testDriver.createInputTopic("laptime", new StringSerializer(), new JsonSerializer<LapTime>());
            var outputTopic = testDriver.createOutputTopic("fastest-lap", new StringDeserializer(), new StringDeserializer());

            inputTopic.pipeInput("2", new LapTime("Lewis Hamilton", "Silverstone", 11L));
            inputTopic.pipeInput("3", new LapTime("Lewis Hamilton", "Silverstone", 13L));
            inputTopic.pipeInput("4", new LapTime("Lewis Hamilton", "Silverstone", 10L));
            inputTopic.pipeInput("5", new LapTime("Lewis Hamilton", "Silverstone", 14L));

            assertThat(outputTopic.readKeyValuesToList()).hasSize(4);

            Object fastestLap = testDriver.getKeyValueStore("fastest-lap-store").get("Lewis Hamilton-Silverstone");

            assertThat(fastestLap).isEqualTo(new LapTime("Lewis Hamilton", "Silverstone", 10L));
        }
    }

}
