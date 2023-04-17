package com.racing.persistence;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class LapTimeListener {

    private final LapTimeRepository lapTimeRepository;

    public LapTimeListener(LapTimeRepository lapTimeRepository) {
        this.lapTimeRepository = lapTimeRepository;
    }

    @KafkaListener(topics = "laptime", groupId = "persistence")
    public void listen(LapTime lapTime) {
        var entity = new LapTimeEntity(lapTime.driver(), lapTime.track(), lapTime.time());
        lapTimeRepository.save(entity);
    }

}
