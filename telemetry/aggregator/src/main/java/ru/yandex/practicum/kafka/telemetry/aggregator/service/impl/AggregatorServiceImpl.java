package ru.yandex.practicum.kafka.telemetry.aggregator.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.aggregator.service.AggregatorService;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Service
@Slf4j
@RequiredArgsConstructor
public class AggregatorServiceImpl implements AggregatorService {
    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    @Override
    public Optional<SensorsSnapshotAvro> aggregate(SensorEventAvro event) {
        String hubId = event.getHubId();
        String sensorId = event.getId();

        SensorsSnapshotAvro snapshot = snapshots.get(hubId);

        if (snapshot == null) {
            snapshot = SensorsSnapshotAvro.newBuilder()
                    .setHubId(hubId)
                    .setTimestamp(event.getTimestamp())
                    .setSensorsState(new HashMap<>())
                    .build();
            log.debug("Новый снапшот для hub = {}", hubId);

        }

        SensorStateAvro oldState = snapshot.getSensorsState().get(sensorId);

        if (oldState != null) {
            if (oldState.getTimestamp().isAfter(event.getTimestamp())) {
                log.debug("Устаревшее событие для датчика {}", sensorId);
                return Optional.empty();
            }

            if (oldState.getData().equals(event.getPayload())) {
                log.debug("Данные датчика {} не изменились", sensorId);
                return Optional.empty();
            }
        }

        Map<String, SensorStateAvro> updatedStates = new HashMap<>(snapshot.getSensorsState());

        updatedStates.put(sensorId, SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build());

        SensorsSnapshotAvro updated = SensorsSnapshotAvro.newBuilder()
                .setHubId(hubId)
                .setTimestamp(event.getTimestamp())
                .setSensorsState(updatedStates)
                .build();

        snapshots.put(hubId, updated);

        log.debug("Снапшот hub = {} обновлён, датчиков: {}", hubId, updatedStates.size());

        return Optional.of(updated);
    }
}
