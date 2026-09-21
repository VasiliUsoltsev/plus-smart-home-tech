package ru.yandex.practicum.kafka.telemetry.analyzer.service;

import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;

public interface HubEventService {
    void addDevice(String hubId, DeviceAddedEventAvro event);

    void deleteDevice(String hubId, String sensorId);

    void addScenario(String hubId, ScenarioAddedEventAvro event);

    void deleteScenario(String hubId, String name);
}
