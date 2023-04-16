package com.racing.ingestion;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/laptime")
public class LapTimeController {

    private final KafkaTemplate<String, LapTime> kafkaTemplate;

    public LapTimeController(KafkaTemplate<String, LapTime> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping
    public void postLapTime(@RequestBody LapTime lapTime) {
        kafkaTemplate.send("laptime", lapTime);
    }

}
