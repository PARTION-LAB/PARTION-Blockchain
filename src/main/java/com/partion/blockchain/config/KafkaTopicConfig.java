package com.partion.blockchain.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String LEDGER_EVENTS = "partion.ledger.events";

    @Bean
    public NewTopic ledgerEventsTopic() {
        return TopicBuilder.name(LEDGER_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
