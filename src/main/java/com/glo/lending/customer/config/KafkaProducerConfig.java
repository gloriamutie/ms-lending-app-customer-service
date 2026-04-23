package com.glo.lending.customer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer configuration for Customer Service.
 * Configures idempotent producer and auto-creates lending.customer.events topic.
 */
@Configuration
@EnableKafka
public class KafkaProducerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerConfig.class);
    private static final int TOPIC_REPLICATION_FACTOR = 1;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.topic.loan-events}")
    private String customerEventsTopic;

    @Value("${app.kafka.topic.partitions}")
    private int topicPartitions;

    /**
     * Produces an idempotent Kafka producer factory.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        log.info("Kafka ProducerFactory configured: bootstrapServers={}", bootstrapServers);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Kafka template for sending messages.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());
        log.info("Kafka template initialized");
        return template;
    }

    /**
     * Auto-creates the customer events topic on startup using values from application.properties.
     */
    @Bean
    public NewTopic customerEventsTopic() {
        log.info("Creating Kafka topic: {} with {} partitions", customerEventsTopic, topicPartitions);
        return TopicBuilder.name(customerEventsTopic)
                .partitions(topicPartitions)
                .replicas(TOPIC_REPLICATION_FACTOR)
                .build();
    }
}
