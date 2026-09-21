package ru.yandex.practicum.kafka.telemetry.analyzer.service.impl;

import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.analyzer.model.Condition;
import ru.yandex.practicum.kafka.telemetry.analyzer.model.Scenario;
import ru.yandex.practicum.kafka.telemetry.analyzer.model.ScenarioAction;
import ru.yandex.practicum.kafka.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.kafka.telemetry.analyzer.service.SnapshotService;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotServiceImpl implements SnapshotService {
    private final ScenarioRepository scenarioRepository;
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    @Override
    @Transactional(readOnly = true)
    public void analyze(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        log.debug("Анализ снапшота hub {}: {} датчиков", hubId, snapshot.getSensorsState().size());

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        if (scenarios.isEmpty()) {
            log.debug("Нет сценариев для hub {}", hubId);
            return;
        }

        Map<String, SensorStateAvro> states = snapshot.getSensorsState();

        for (Scenario scenario : scenarios) {
            if (!allConditionsMet(scenario, states)) {
                continue;
            }

            log.info("Сценарий '{}' hub {} сработал", scenario.getName(), hubId);

            for (ScenarioAction action : scenario.getActions()) {
                executeAction(snapshot, scenario, action);
            }
        }
    }

    // Проверка условий

    private boolean allConditionsMet(Scenario scenario, Map<String, SensorStateAvro> states) {
        return scenario.getConditions().stream()
                .allMatch(sc -> isConditionMet(
                        sc.getCondition(),
                        states.get(sc.getSensor().getId())));
    }

    private boolean isConditionMet(Condition condition, SensorStateAvro state) {
        if (state == null || condition.getValue() == null) {
            return false;
        }

        return extractValue(condition.getType(), state.getData())
                .map(actual -> compare(condition.getOperation(), actual, condition.getValue()))
                .orElse(false);
    }

    private Optional<Integer> extractValue(ConditionTypeAvro type, Object data) {
        return switch (type) {
            case TEMPERATURE -> {
                if (data instanceof TemperatureSensorAvro t) {
                    yield Optional.of(t.getTemperatureC());
                } else if (data instanceof ClimateSensorAvro c) {
                    yield Optional.of(c.getTemperatureC());
                }
                yield Optional.empty();
            }
            case LUMINOSITY -> data instanceof LightSensorAvro l
                    ? Optional.of(l.getLuminosity()) : Optional.empty();
            case MOTION -> data instanceof MotionSensorAvro m
                    ? Optional.of(m.getMotion() ? 1 : 0) : Optional.empty();
            case SWITCH -> data instanceof SwitchSensorAvro s
                    ? Optional.of(s.getState() ? 1 : 0) : Optional.empty();
            case CO2LEVEL -> data instanceof ClimateSensorAvro c
                    ? Optional.of(c.getCo2Level()) : Optional.empty();
            case HUMIDITY -> data instanceof ClimateSensorAvro c
                    ? Optional.of(c.getHumidity()) : Optional.empty();
        };
    }

    private boolean compare(ConditionOperationAvro operation, int actual, int expected) {
        return switch (operation) {
            case EQUALS -> actual == expected;
            case GREATER_THAN -> actual > expected;
            case LOWER_THAN -> actual < expected;
        };
    }

    // Отправка действий

    private void executeAction(SensorsSnapshotAvro snapshot,
                               Scenario scenario,
                               ScenarioAction action) {
        DeviceActionRequest request = DeviceActionRequest.newBuilder()
                .setHubId(snapshot.getHubId().toString())
                .setScenarioName(scenario.getName())
                .setAction(toProtoAction(action))
                .setTimestamp(toProtoTimestamp(snapshot.getTimestamp()))
                .build();

        try {
            hubRouterClient.handleDeviceAction(request);
            log.info("Команда отправлена: hub={}, сценарий='{}', датчик={}, тип={}, value={}",
                    snapshot.getHubId(),
                    scenario.getName(),
                    action.getSensor().getId(),
                    action.getAction().getType(),
                    action.getAction().getValue());
        } catch (StatusRuntimeException e) {
            log.error("Ошибка gRPC при отправке команды: {}", e.getStatus().getCode(), e);
        }
    }

    private DeviceActionProto toProtoAction(ScenarioAction action) {
        DeviceActionProto.Builder builder = DeviceActionProto.newBuilder()
                .setSensorId(action.getSensor().getId())
                .setType(ActionTypeProto.valueOf(action.getAction().getType().name()));

        if (action.getAction().getValue() != null) {
            builder.setValue(action.getAction().getValue());
        }

        return builder.build();
    }

    private Timestamp toProtoTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}