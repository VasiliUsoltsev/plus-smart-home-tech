package ru.yandex.practicum.kafka.telemetry.analyzer.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.analyzer.model.*;
import ru.yandex.practicum.kafka.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.kafka.telemetry.analyzer.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.analyzer.service.HubEventService;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HubEventServiceImpl implements HubEventService {
    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;

    @Override
    @Transactional
    public void addDevice(String hubId, DeviceAddedEventAvro event) {
        String sensorId = event.getId();
        Optional<Sensor> optSensor = sensorRepository.findByIdAndHubId(sensorId, hubId);

        if (optSensor.isEmpty()) {
            Sensor sensor = Sensor.builder()
                    .id(sensorId)
                    .hubId(hubId)
                    .build();
            sensorRepository.save(sensor);
            log.debug("Новый девайс {} добавлен в hub {}", sensorId, hubId);
        }
    }

    @Override
    @Transactional
    public void deleteDevice(String hubId, String sensorId) {
        Optional<Sensor> optSensor = sensorRepository.findByIdAndHubId(sensorId, hubId);

        if (optSensor.isPresent()) {
            Sensor sensor = Sensor.builder()
                    .id(sensorId)
                    .hubId(hubId)
                    .build();
            sensorRepository.delete(optSensor.get());
            log.debug("Девайс {} удален из hub {}", sensorId, hubId);


        }
    }

    @Override
    @Transactional
    public void addScenario(String hubId, ScenarioAddedEventAvro event) {
        String name = event.getName().toString();

        Scenario scenario = scenarioRepository.findByHubIdAndName(hubId, name)
                .orElseGet(() -> Scenario.builder()
                        .hubId(hubId)
                        .name(name)
                        .build());

        scenario.getConditions().clear();
        scenario.getActions().clear();

        // Собрать все sensorId из условий и действий
        Set<String> sensorIds = collectSensorIds(event);

        // Загрузить все датчики одним запросом
        Map<String, Sensor> sensors = loadSensors(sensorIds, hubId);

        // Заполнить условия
        event.getConditions().forEach(condition -> {
            Sensor sensor = sensors.get(condition.getSensorId().toString());
            if (sensor == null) {
                log.debug("Датчик {} не найден в hub {}, условие пропущено",
                        condition.getSensorId(), hubId);
                return;
            }

            scenario.getConditions().add(
                    buildScenarioCondition(scenario, sensor, condition));
        });

        // Заполнить действия
        event.getActions().forEach(action -> {
            Sensor sensor = sensors.get(action.getSensorId().toString());
            if (sensor == null) {
                log.debug("Датчик {} не найден в hub {}, действие пропущено",
                        action.getSensorId(), hubId);
                return;
            }

            scenario.getActions().add(
                    buildScenarioAction(scenario, sensor, action));
        });

        // Сохранить
        scenarioRepository.save(scenario);

        log.debug("Сценарий сохранен'{}': условия={}, действия={}",
                name, scenario.getConditions().size(), scenario.getActions().size());
    }

    @Override
    @Transactional
    public void deleteScenario(String hubId, String name) {
        Optional<Scenario> optScenario = scenarioRepository.findByHubIdAndName(hubId, name);

        if (optScenario.isPresent()) {
            scenarioRepository.delete(optScenario.get());
            log.debug("Сценарий удален '{}'", name);
        }
    }

    private Set<String> collectSensorIds(ScenarioAddedEventAvro event) {
        Set<String> sensorIds = new HashSet<>();
        event.getConditions().forEach(c -> sensorIds.add(c.getSensorId().toString()));
        event.getActions().forEach(a -> sensorIds.add(a.getSensorId().toString()));
        return sensorIds;
    }

    private Map<String, Sensor> loadSensors(Set<String> sensorIds, String hubId) {
        return sensorRepository.findAllByIdInAndHubId(sensorIds, hubId)
                .stream()
                .collect(Collectors.toMap(Sensor::getId, Function.identity()));
    }

    private ScenarioCondition buildScenarioCondition(Scenario scenario,
                                                     Sensor sensor,
                                                     ScenarioConditionAvro avro) {
        Condition condition = Condition.builder()
                .type(ConditionTypeAvro.valueOf(avro.getType().name()))
                .operation(ConditionOperationAvro.valueOf(avro.getOperation().name()))
                .value(toInt(avro.getValue()))
                .build();

        return ScenarioCondition.builder()
                .scenario(scenario)
                .sensor(sensor)
                .condition(condition)
                .build();
    }

    private ScenarioAction buildScenarioAction(Scenario scenario,
                                               Sensor sensor,
                                               DeviceActionAvro avro) {
        Action action = Action.builder()
                .type(ActionTypeAvro.valueOf(avro.getType().name()))
                .value(avro.getValue())
                .build();

        return ScenarioAction.builder()
                .scenario(scenario)
                .sensor(sensor)
                .action(action)
                .build();
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Boolean b) {
            return b ? 1 : 0;
        }
        throw new IllegalArgumentException("Неподдерживаемый тип value: " + value.getClass());
    }
}
