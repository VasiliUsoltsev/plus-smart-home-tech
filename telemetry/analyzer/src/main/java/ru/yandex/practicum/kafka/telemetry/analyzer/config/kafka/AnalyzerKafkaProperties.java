package ru.yandex.practicum.kafka.telemetry.analyzer.config.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka")
@Getter
@Setter
public class AnalyzerKafkaProperties {
    private String bootstrapServers;

    private String snapshotConsumerGroupId;
    private String snapshotConsumerTopic;
    private String snapshotConsumerKeyDeserializer;
    private String snapshotConsumerValueDeserializer;
    private String snapshotConsumerEnableAutoCommitConfig;
    private String snapshotConsumerAutoOffsetResetConfig;
    private String snapshotConsumerMaxPollRecordsConfig;

    private String hubEventConsumerGroupId;
    private String hubEventConsumerTopic;
    private String hubEventConsumerKeyDeserializer;
    private String hubEventConsumerValueDeserializer;
    private String hubEventConsumerEnableAutoCommitConfig;
    private String hubEventConsumerAutoOffsetResetConfig;
    private String hubEventConsumerMaxPollRecordsConfig;
}
