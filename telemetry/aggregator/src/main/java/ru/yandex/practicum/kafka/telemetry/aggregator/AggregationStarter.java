package ru.yandex.practicum.kafka.telemetry.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.aggregator.config.kafka.AggregatorKafkaProperties;
import ru.yandex.practicum.kafka.telemetry.aggregator.exception.KafkaSendException;
import ru.yandex.practicum.kafka.telemetry.aggregator.service.AggregatorService;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {
    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);
    private static final long SEND_TIMEOUT_SECONDS = 10L;
    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaProducer<String, SpecificRecordBase> producer;
    private final AggregatorService aggregatorService;
    private final AggregatorKafkaProperties properties;

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        log.debug("Начинаем опрашивать топик {}", properties.getSnapshotTopic());

        try {
            consumer.subscribe(List.of(properties.getSensorTopic()));

            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);

                int count = 0;

                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    handleRecord(record);
                    manageOffsets(record, count, consumer);
                    count++;
                }

                consumer.commitAsync();

            }
        } catch (WakeupException ignored) {

        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                producer.flush();
                consumer.commitSync(currentOffsets);
            } finally {
                log.debug("Закрываем поставщика");
                producer.close();

                log.debug("Закрываем потребителя");
                consumer.close();
            }
        }
    }

    private void handleRecord(ConsumerRecord<String, SensorEventAvro> inputRecord) throws InterruptedException {
        log.debug("топик = {}, партиция = {}, смещение = {}, значение: {}\n",
                inputRecord.topic(), inputRecord.partition(), inputRecord.offset(), inputRecord.value());

        Optional<SensorsSnapshotAvro> result = aggregatorService.aggregate(inputRecord.value());

        if (result.isPresent()) {
            SensorsSnapshotAvro snapshot = result.get();

            ProducerRecord<String, SpecificRecordBase> outputRecord = new ProducerRecord<>(
                    properties.getSnapshotTopic(),
                    null,
                    snapshot.getTimestamp().toEpochMilli(),
                    snapshot.getHubId(),
                    snapshot
            );

            try {
                Future<RecordMetadata> send = producer.send(outputRecord);
                producer.flush();
                RecordMetadata metadata = send.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                log.debug("Снапшот успешно отправлен: topic={}, hubId={}, offset={}",
                        metadata.topic(),
                        snapshot.getHubId(),
                        metadata.offset()
                );

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Прервана отправка снапшота: topic={}, id={}",
                        properties.getSnapshotTopic(),
                        snapshot.getHubId(),
                        e
                );
                throw new KafkaSendException("Прервана отправка снапшота " + snapshot.getHubId());

            } catch (TimeoutException e) {
                log.error("Таймаут ожидания Kafka: topic={}, id={}",
                        properties.getSnapshotTopic(),
                        snapshot.getHubId(),
                        e
                );
                throw new KafkaSendException(
                        "Kafka не ответила вовремя на снапшот " + snapshot.getHubId());

            } catch (ExecutionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                log.error("Ошибка при отправке снапшота: topic={}, hubId={}",
                        properties.getSnapshotTopic(),
                        snapshot.getHubId(),
                        cause
                );
                throw new KafkaSendException(
                        "Не удалось отправить снапшот " + snapshot.getHubId());
            }
        }
    }

    private void manageOffsets(ConsumerRecord<String, SensorEventAvro> record,
                               int count,
                               KafkaConsumer<String, SensorEventAvro> consumer
    ) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        consumer.commitAsync(currentOffsets, (offsets, exception) -> {
            if (exception != null) {
                log.error("Ошибка во время фиксации оффсетов: {}", offsets, exception);
            }
        });
    }
}
