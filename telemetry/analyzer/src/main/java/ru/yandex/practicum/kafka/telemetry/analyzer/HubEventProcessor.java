package ru.yandex.practicum.kafka.telemetry.analyzer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.analyzer.config.kafka.AnalyzerKafkaProperties;
import ru.yandex.practicum.kafka.telemetry.analyzer.service.HubEventService;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {
    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);

    private final KafkaConsumer<String, HubEventAvro> consumer;
    private final HubEventService hubEventService;
    private final AnalyzerKafkaProperties properties;

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        log.debug("Начинаем опрашивать топик {}", properties.getHubEventConsumerTopic());

        try {
            consumer.subscribe(List.of(properties.getHubEventConsumerTopic()));

            while (true) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    handleRecord(record.value());
                }

                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException e) {

        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от hub", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                log.debug("Завершаем опрашивать топик {}", properties.getHubEventConsumerTopic());
                consumer.close();
            }
        }
    }

    private void handleRecord(HubEventAvro event) {
        Object payload = event.getPayload();

        if (payload instanceof DeviceAddedEventAvro addEvent) {
            hubEventService.addDevice(event.getHubId(), addEvent);
        } else if (payload instanceof DeviceRemovedEventAvro deleteEvent) {
            hubEventService.deleteDevice(event.getHubId(), deleteEvent.getId());
        } else if (payload instanceof ScenarioAddedEventAvro addEvent) {
            hubEventService.addScenario(event.getHubId(), addEvent);
        } else if (payload instanceof ScenarioRemovedEventAvro deleteEvent) {
            hubEventService.deleteScenario(event.getHubId(), deleteEvent.getName());
        } else {
            log.warn("Неизвестный тип события hub: {}", payload.getClass().getName());
        }
    }
}
