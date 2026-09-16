package ru.yandex.practicum.kafka.telemetry.collector.service;

import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events.HubEvent;
import ru.yandex.practicum.kafka.telemetry.collector.model.sensors.SensorEvent;

public interface CollectorService {
    void sendSensorEvent(SensorEvent event);

    void sendHubEvent(HubEvent event);
}
