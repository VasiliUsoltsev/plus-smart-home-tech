package ru.yandex.practicum.kafka.telemetry.aggregator.service;

import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.Optional;

public interface AggregatorService {
    Optional<SensorsSnapshotAvro> aggregate(SensorEventAvro event);
}
