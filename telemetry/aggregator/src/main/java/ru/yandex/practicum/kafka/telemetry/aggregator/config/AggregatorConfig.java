package ru.yandex.practicum.kafka.telemetry.aggregator.config;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.aggregator.config.kafka.AggregatorKafkaProperties;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class AggregatorConfig {
    private final AggregatorKafkaProperties properties;

    @Bean
    public KafkaProducer<String, SpecificRecordBase> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, properties.getKeySerializer());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, properties.getValueSerializer());

        return new KafkaProducer<>(config);
    }

    @Bean
    public KafkaConsumer<String, SensorEventAvro> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, "AggregatorConsumer");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getGroup());
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, properties.getKeyDeserializer());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, properties.getValueDeserializer());
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, properties.getMaxPoll());

        return new KafkaConsumer<>(config);
    }
}
