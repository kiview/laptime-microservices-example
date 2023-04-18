package com.racing.fastestlap;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic topic() {
        return TopicBuilder.name("laptime")
                .partitions(10)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic anotherTopic() {
        return TopicBuilder.name("fastest-lap")
                .partitions(10)
                .replicas(1)
                .build();
    }

}

