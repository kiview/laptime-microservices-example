package com.racing.fastestlap;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreType;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fastest-lap")
public class FastestLapController {

    private final StreamsBuilderFactoryBean factoryBean;

    public FastestLapController(StreamsBuilderFactoryBean factoryBean) {
        this.factoryBean = factoryBean;
    }

    @GetMapping("/{driver}/{track}")
    public LapTime getFastestLap(@PathVariable String driver, @PathVariable String track) {
        var kafkaStreams = factoryBean.getKafkaStreams();
        driver = driver.replace("_", " ");
        ReadOnlyKeyValueStore<String, LapTime> valueStore =
                kafkaStreams.store(StoreQueryParameters.fromNameAndType("fastest-lap-store", QueryableStoreTypes.keyValueStore()));

        var key = driver + "-" + track;

        return valueStore.get(key);
    }

}
