package ru.yandex.practicum.kafka.telemetry.aggregator.config.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aggregator.kafka")
@Getter
@Setter
public class AggregatorKafkaProperties {
    private String bootstrapServers;
    private String keySerializer;
    private String valueSerializer;
    private String keyDeserializer;
    private String valueDeserializer;
    private String sensorTopic;
    private String snapshotTopic;
    private String maxPoll;
    private String group;
}
