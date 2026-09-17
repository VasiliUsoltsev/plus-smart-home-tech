package ru.yandex.practicum.kafka.telemetry.collector.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.DeviceAction;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events.DeviceAddedEvent;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.ScenarioCondition;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events.DeviceRemovedEvent;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events.HubEvent;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events.ScenarioAddedEvent;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events.ScenarioRemovedEvent;
import ru.yandex.practicum.kafka.telemetry.collector.model.sensors.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CollectorMapper {
    public static SensorEventAvro mapToSensorEventAvro(SensorEvent event) {
        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp());

        switch (event.getType()) {
            case LIGHT_SENSOR_EVENT:
                LightSensorEvent lightSensor = (LightSensorEvent) event;

                LightSensorAvro lightSensorAvro = LightSensorAvro.newBuilder()
                        .setLinkQuality(lightSensor.getLinkQuality())
                        .setLuminosity(lightSensor.getLuminosity())
                        .build();

                builder.setPayload(lightSensorAvro);
                break;

            case MOTION_SENSOR_EVENT:
                MotionSensorEvent motionSensor = (MotionSensorEvent) event;

                MotionSensorAvro motionSensorAvro = MotionSensorAvro.newBuilder()
                        .setMotion(motionSensor.getMotion())
                        .setLinkQuality(motionSensor.getLinkQuality())
                        .setVoltage(motionSensor.getVoltage())
                        .build();

                builder.setPayload(motionSensorAvro);
                break;

            case SWITCH_SENSOR_EVENT:
                SwitchSensorEvent switchSensor = (SwitchSensorEvent) event;

                SwitchSensorAvro switchSensorAvro = SwitchSensorAvro.newBuilder()
                        .setState(switchSensor.getState())
                        .build();

                builder.setPayload(switchSensorAvro);
                break;

            case CLIMATE_SENSOR_EVENT:
                ClimateSensorEvent climateSensor = (ClimateSensorEvent) event;

                ClimateSensorAvro climateSensorAvro = ClimateSensorAvro.newBuilder()
                        .setCo2Level(climateSensor.getCo2Level())
                        .setHumidity(climateSensor.getHumidity())
                        .setTemperatureC(climateSensor.getTemperatureC())
                        .build();

                builder.setPayload(climateSensorAvro);
                break;

            case TEMPERATURE_SENSOR_EVENT:
                TemperatureSensorEvent temperatureSensor = (TemperatureSensorEvent) event;

                TemperatureSensorAvro temperatureSensorAvro = TemperatureSensorAvro.newBuilder()
                        .setTemperatureC(temperatureSensor.getTemperatureC())
                        .setTemperatureF(temperatureSensor.getTemperatureF())
                        .build();

                builder.setPayload(temperatureSensorAvro);
                break;
            default:
                throw new IllegalArgumentException("Тип сенсора нераспознан: " + event.getType().name());
        }

        return builder.build();
    }

    public static HubEventAvro mapToHubEventAvro(HubEvent event) {
        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp());

        switch (event.getType()) {
            case DEVICE_ADDED:
                DeviceAddedEvent deviceAdded = (DeviceAddedEvent) event;

                DeviceAddedEventAvro deviceAddedEventAvro = DeviceAddedEventAvro.newBuilder()
                        .setId(deviceAdded.getId())
                        .setType(DeviceTypeAvro.valueOf(deviceAdded.getDeviceType()))
                        .build();

                builder.setPayload(deviceAddedEventAvro);
                break;

            case DEVICE_REMOVED:
                DeviceRemovedEvent deviceRemoved = (DeviceRemovedEvent) event;

                DeviceRemovedEventAvro deviceRemovedEventAvro = DeviceRemovedEventAvro.newBuilder()
                        .setId(deviceRemoved.getId())
                        .build();

                builder.setPayload(deviceRemovedEventAvro);
                break;

            case SCENARIO_ADDED:
                ScenarioAddedEvent scenarioAdded = (ScenarioAddedEvent) event;

                List<ScenarioConditionAvro> conditions = new ArrayList<>();
                for (ScenarioCondition condition : scenarioAdded.getConditions()) {
                    ScenarioConditionAvro scenarioConditionAvro = ScenarioConditionAvro.newBuilder()
                            .setSensorId(condition.getSensorId())
                            .setType(ConditionTypeAvro.valueOf(condition.getType()))
                            .setOperation(ConditionOperationAvro.valueOf(condition.getOperation()))
                            .setValue(condition.getValue())
                            .build();
                    conditions.add(scenarioConditionAvro);
                }

                List<DeviceActionAvro> actions = new ArrayList<>();
                for (DeviceAction action : scenarioAdded.getActions()) {
                    DeviceActionAvro deviceActionAvro = DeviceActionAvro.newBuilder()
                            .setSensorId(action.getSensorId())
                            .setType(ActionTypeAvro.valueOf(action.getType()))
                            .setValue(action.getValue())
                            .build();
                    actions.add(deviceActionAvro);
                }

                ScenarioAddedEventAvro scenarioAddedEventAvro = ScenarioAddedEventAvro.newBuilder()
                        .setName(scenarioAdded.getName())
                        .setConditions(conditions)
                        .setActions(actions)
                        .build();

                builder.setPayload(scenarioAddedEventAvro);
                break;

            case SCENARIO_REMOVED:
                ScenarioRemovedEvent scenarioRemoved = (ScenarioRemovedEvent) event;

                ScenarioRemovedEventAvro scenarioRemovedEventAvro = ScenarioRemovedEventAvro.newBuilder()
                        .setName(scenarioRemoved.getName())
                        .build();

                builder.setPayload(scenarioRemovedEventAvro);
                break;

            default:
                throw new IllegalArgumentException("Тип события хаба нераспознан: " + event.getType().name());
        }

        return builder.build();
    }
}
