package ru.yandex.practicum.kafka.telemetry.analyzer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.analyzer.config.kafka.AnalyzerKafkaProperties;
import ru.yandex.practicum.kafka.telemetry.analyzer.service.SnapshotService;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {
    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);

    private final KafkaConsumer<String, SensorsSnapshotAvro> consumer;
    private final AnalyzerKafkaProperties properties;
    private final SnapshotService snapshotService;

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        log.debug("Начинаем опрашивать топик {}", properties.getSnapshotConsumerTopic());

        try {
            consumer.subscribe(List.of(properties.getSnapshotConsumerTopic()));

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    snapshotService.analyze(record.value());
                    currentOffsets.put(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1)
                    );
                }

                if (!records.isEmpty()) {
                    consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                        if (exception != null) {
                            log.error("Ошибка commit: {}", offsets, exception);
                        }
                    });
                }
            }
        } catch (WakeupException e) {

        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от snapshot", e);
        } finally {
            try {
                consumer.commitAsync();
            } finally {
                log.debug("Завершаем опрашивать топик {}", properties.getSnapshotConsumerTopic());
                consumer.close();
            }
        }
    }
}
