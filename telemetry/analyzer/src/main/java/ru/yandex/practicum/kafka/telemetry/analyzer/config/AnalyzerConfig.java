package ru.yandex.practicum.kafka.telemetry.analyzer.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.analyzer.config.grps.AnalyzerGrpsProperties;
import ru.yandex.practicum.kafka.telemetry.analyzer.config.kafka.AnalyzerKafkaProperties;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class AnalyzerConfig {
    private final AnalyzerKafkaProperties propertiesKafka;
    private final AnalyzerGrpsProperties propertiesGrps;

    @Bean(destroyMethod = "")
    public KafkaConsumer<String, HubEventAvro> consumerHubEvent() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, "consumerHubEvent");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, propertiesKafka.getHubEventConsumerGroupId());
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, propertiesKafka.getBootstrapServers());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, propertiesKafka.getHubEventConsumerKeyDeserializer());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, propertiesKafka.getHubEventConsumerValueDeserializer());
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, propertiesKafka.getHubEventConsumerMaxPollRecordsConfig());

        return new KafkaConsumer<>(config);
    }

    @Bean(destroyMethod = "")
    public KafkaConsumer<String, SensorsSnapshotAvro> consumerSnapshot() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, "consumerSnapshot");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, propertiesKafka.getSnapshotConsumerGroupId());
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, propertiesKafka.getBootstrapServers());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, propertiesKafka.getSnapshotConsumerKeyDeserializer());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, propertiesKafka.getSnapshotConsumerValueDeserializer());
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, propertiesKafka.getSnapshotConsumerMaxPollRecordsConfig());

        return new KafkaConsumer<>(config);
    }

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel hubRouterChannel() {
        String address = propertiesGrps.getAddress()
                .replace("static://", "");
        String[] parts = address.split(":");

        return ManagedChannelBuilder
                .forAddress(parts[0], Integer.parseInt(parts[1]))
                .usePlaintext()
                .build();
    }

    @Bean
    public HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient(
            ManagedChannel hubRouterAddress) {
        return HubRouterControllerGrpc.newBlockingStub(hubRouterAddress);
    }
}
