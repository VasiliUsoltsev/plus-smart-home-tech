package ru.yandex.practicum.kafka.telemetry.collector.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.collector.config.kafka.CollectorKafkaProperties;
import ru.yandex.practicum.kafka.telemetry.collector.exception.KafkaSendException;
import ru.yandex.practicum.kafka.telemetry.collector.mapper.CollectorMapper;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events.HubEvent;
import ru.yandex.practicum.kafka.telemetry.collector.model.sensors.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.collector.service.CollectorService;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollectorServiceImpl implements CollectorService,AutoCloseable {
    private static final long SEND_TIMEOUT_SECONDS = 10L;

    private final KafkaProducer<String, SpecificRecordBase> producer;
    private final CollectorKafkaProperties properties;

    @Override
    public void sendSensorEvent(SensorEvent event) {
        SensorEventAvro avro = CollectorMapper.mapToSensorEventAvro(event);

        send(
                properties.getSensorTopic(),
                event.getType().toString(),
                event.getTimestamp().toEpochMilli(),
                avro,
                "датчика",
                event.getId()
        );
    }

    @Override
    public void sendHubEvent(HubEvent event) {
        HubEventAvro avro = CollectorMapper.mapToHubEventAvro(event);

        send(
                properties.getHubTopic(),
                event.getType().toString(),
                event.getTimestamp().toEpochMilli(),
                avro,
                "хаба",
                event.getHubId()
        );
    }

    private void send(String topic,
                      String key,
                      long timestamp,
                      SpecificRecordBase value,
                      String eventType,
                      String eventId) {

        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                topic,
                null,
                timestamp,
                key,
                value
        );

        try {
            Future<RecordMetadata> send = producer.send(record);
            producer.flush();
            RecordMetadata metadata = send.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("Событие {} успешно отправлено: topic={}, typeEvent={}, partition={}, offset={}",
                    eventType,
                    metadata.topic(),
                    key,
                    metadata.partition(),
                    metadata.offset()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Прервана отправка события {}: topic={}, id={}",
                    eventType, topic, eventId, e);
            throw new KafkaSendException("Прервана отправка события " + eventType, e);

        } catch (TimeoutException e) {
            log.error("Таймаут ожидания Kafka: topic={}, id={}",
                    topic, eventId, e);
            throw new KafkaSendException(
                    "Kafka не ответила вовремя на событие " + eventType, e);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Ошибка при отправке события {}: topic={}, id={}",
                    eventType, topic, eventId, cause);
            throw new KafkaSendException(
                    "Не удалось отправить событие " + eventType, cause);
        }
    }

    @Override
    public void close() {
        producer.flush();
        producer.close(Duration.ofSeconds(10));
    }
}