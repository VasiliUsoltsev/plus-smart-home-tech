package ru.yandex.practicum.kafka.telemetry.collector.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.collector.config.kafka.CollectorKafkaTopics;
import ru.yandex.practicum.kafka.telemetry.collector.exception.KafkaSendException;
import ru.yandex.practicum.kafka.telemetry.collector.mapper.CollectorMapper;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events.HubEvent;
import ru.yandex.practicum.kafka.telemetry.collector.model.sensors.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.collector.service.CollectorService;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollectorServiceImpl implements CollectorService {
    private final KafkaProducer<String, SpecificRecordBase> producer;

    @Override
    public void sendSensorEvent(SensorEvent event) {
        SensorEventAvro sensorEventAvro = CollectorMapper.mapToSensorEventAvro(event);
        long timestamp = event.getTimestamp().toEpochMilli();
        String key = event.getType().toString();
        String topic = CollectorKafkaTopics.sensorTopic;

        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                topic,
                null,
                timestamp,
                key,
                sensorEventAvro
        );

        try {
            Future<RecordMetadata> send = producer.send(record);
            RecordMetadata metadata = send.get(10L, TimeUnit.SECONDS);

            log.info("Событие датчика успешно отправлено: topic={}, typeEvent={}",
                    metadata.topic(), event.getType().name()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Прервана отправка события датчика: topic={}, id={}, ",
                    topic,
                    event.getId(),
                    e
            );

            throw new KafkaSendException("Прервана отправка события датчика", e);

        } catch (TimeoutException e) {
            log.error("Таймаут ожидания Kafka: topic={}, id={}, ",
                    topic,
                    event.getId(),
                    e
            );

            throw new KafkaSendException("Kafka не ответила вовремя на событие датчика", e);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Произошла ошибка при отправке события датчика: topic={}, id={}, ",
                    topic,
                    event.getId(),
                    cause);

            throw new KafkaSendException("Не удалось отправить событие датчика", cause);
        }
    }

    @Override
    public void sendHubEvent(HubEvent event) {
        HubEventAvro hubEventAvro = CollectorMapper.mapToHubEventAvro(event);
        long timestamp = event.getTimestamp().toEpochMilli();
        String key = event.getType().toString();
        String topic = CollectorKafkaTopics.hubTopic;

        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                topic,
                null,
                timestamp,
                key,
                hubEventAvro
        );

        try {
            Future<RecordMetadata> send = producer.send(record);
            RecordMetadata metadata = send.get(10L, TimeUnit.SECONDS);

            log.info("Событие хаба успешно отправлено: topic={}, typeEvent={}",
                    metadata.topic(), event.getType().name()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Прервана отправка события хаба: topic={}, hubId={}, ",
                    topic,
                    event.getHubId(),
                    e
            );

            throw new KafkaSendException("Прервана отправка события хаба", e);

        } catch (TimeoutException e) {
            log.error("Таймаут ожидания Kafka: topic={}, hubId={}, ",
                    topic,
                    event.getHubId(),
                    e
            );

            throw new KafkaSendException("Kafka не ответила вовремя на событие хаба", e);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Произошла ошибка при отправке события хаба: topic={}, hubId={}, ",
                    topic,
                    event.getHubId(),
                    cause);

            throw new KafkaSendException("Не удалось отправить событие хаба", cause);
        }
    }
}