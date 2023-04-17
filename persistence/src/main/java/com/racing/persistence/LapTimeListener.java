package com.racing.persistence;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class LapTimeListener {

    private final LapTimeRepository lapTimeRepository;

    private final Timer timer;

    public LapTimeListener(LapTimeRepository lapTimeRepository, MeterRegistry registry) {
        this.lapTimeRepository = lapTimeRepository;
        timer = registry.timer("persistence.laptime", "type", "save");
    }

    @KafkaListener(topics = "laptime", groupId = "persistence")
    public void listen(LapTime lapTime) {
        var entity = new LapTimeEntity(lapTime.driver(), lapTime.track(), lapTime.time());
        timer.record(() -> lapTimeRepository.saveAndFlush(entity));
    }

}
