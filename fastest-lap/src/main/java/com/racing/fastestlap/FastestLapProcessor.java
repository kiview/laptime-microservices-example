package com.racing.fastestlap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

@Component
public class FastestLapProcessor {



    @Autowired
    void buildFastestLapPipeline(StreamsBuilder builder, ObjectMapper mapper) {

        Serde<LapTime> lapTimeSerde = new JsonSerde<>(LapTime.class, mapper);

        var laptimeStream = builder.stream("laptime",
                Consumed.with(Serdes.String(), lapTimeSerde));

        KTable<String, LapTime> fastestLap = laptimeStream
                .groupBy(
                (key, value) -> value.driver() + "-" + value.track())
                .reduce((aggValue, newValue) -> aggValue.time() < newValue.time() ? aggValue : newValue,
                        Materialized.as("fastest-lap-store"));

        fastestLap.toStream().to("fastest-lap");
    }

}
