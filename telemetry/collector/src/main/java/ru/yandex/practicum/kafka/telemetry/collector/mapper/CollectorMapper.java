package ru.yandex.practicum.kafka.telemetry.collector.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.DeviceAction;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events.DeviceAddedEvent;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.ScenarioCondition;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events.DeviceRemovedEvent;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events.HubEvent;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events.ScenarioAddedEvent;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events.ScenarioRemovedEvent;
import ru.yandex.practicum.kafka.telemetry.collector.model.sensors.*;
import ru.yandex.practicum.kafka.telemetry.collector.model.sensors.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
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
                throw new IllegalArgumentException("Тип sensor нераспознан: " + event.getType().name());
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
                throw new IllegalArgumentException("Тип hub события нераспознан: " + event.getType().name());
        }

        return builder.build();
    }

    public static SensorEvent mapToSensorEvent(SensorEventProto proto) {
        SensorEvent result;

        switch (proto.getPayloadCase()) {
            case MOTION_SENSOR:
                MotionSensorProto motionSensor = proto.getMotionSensor();

                MotionSensorEvent motionSensorEvent = new MotionSensorEvent();
                motionSensorEvent.setLinkQuality(motionSensor.getLinkQuality());
                motionSensorEvent.setMotion(motionSensor.getMotion());
                motionSensorEvent.setVoltage(motionSensor.getVoltage());
                result = motionSensorEvent;
                break;

            case TEMPERATURE_SENSOR:
                TemperatureSensorProto temperatureSensor = proto.getTemperatureSensor();

                TemperatureSensorEvent temperatureSensorEvent = new TemperatureSensorEvent();
                temperatureSensorEvent.setTemperatureC(temperatureSensor.getTemperatureC());
                temperatureSensorEvent.setTemperatureF(temperatureSensor.getTemperatureF());
                result = temperatureSensorEvent;
                break;

            case LIGHT_SENSOR:
                LightSensorProto lightSensor = proto.getLightSensor();

                LightSensorEvent lightSensorEvent = new LightSensorEvent();
                lightSensorEvent.setLinkQuality(lightSensor.getLinkQuality());
                lightSensorEvent.setLuminosity(lightSensor.getLuminosity());
                result = lightSensorEvent;
                break;

            case CLIMATE_SENSOR:
                ClimateSensorProto climateSensor = proto.getClimateSensor();

                ClimateSensorEvent climateSensorEvent = new ClimateSensorEvent();
                climateSensorEvent.setTemperatureC(climateSensor.getTemperatureC());
                climateSensorEvent.setHumidity(climateSensor.getHumidity());
                climateSensorEvent.setCo2Level(climateSensor.getCo2Level());
                result = climateSensorEvent;
                break;

            case SWITCH_SENSOR:
                SwitchSensorProto switchSensor = proto.getSwitchSensor();

                SwitchSensorEvent switchSensorEvent = new SwitchSensorEvent();
                switchSensorEvent.setState(switchSensor.getState());
                result = switchSensorEvent;
                break;

            default:
                throw new IllegalArgumentException(
                        "Тип sensor нераспознан: " + proto.getPayloadCase());
        }

        result.setId(proto.getId());
        result.setHubId(proto.getHubId());
        result.setTimestamp(toInstant(proto.getTimestamp()));

        return result;
    }

    public static HubEvent mapToHubEvent(HubEventProto proto) {
        HubEvent result;

        switch (proto.getPayloadCase()) {
            case DEVICE_ADDED:
                DeviceAddedEventProto deviceAdded = proto.getDeviceAdded();

                DeviceAddedEvent deviceAddedEvent = new DeviceAddedEvent();
                deviceAddedEvent.setId(deviceAdded.getId());
                deviceAddedEvent.setDeviceType(deviceAdded.getType().name());
                result = deviceAddedEvent;
                break;

            case DEVICE_REMOVED:
                DeviceRemovedEventProto deviceRemoved = proto.getDeviceRemoved();

                DeviceRemovedEvent deviceRemovedEvent = new DeviceRemovedEvent();
                deviceRemovedEvent.setId(deviceRemoved.getId());
                result = deviceRemovedEvent;
                break;

            case SCENARIO_ADDED:
                ScenarioAddedEventProto scenarioAdded = proto.getScenarioAdded();

                List<ScenarioCondition> conditions = new ArrayList<>();
                for (ScenarioConditionProto conditionProto : scenarioAdded.getConditionList()) {
                    ScenarioCondition condition = new ScenarioCondition();
                    condition.setSensorId(conditionProto.getSensorId());
                    condition.setType(conditionProto.getType().name());
                    condition.setOperation(conditionProto.getOperation().name());
                    condition.setValue(mapConditionValue(conditionProto));
                    conditions.add(condition);
                }

                List<DeviceAction> actions = new ArrayList<>();
                for (DeviceActionProto actionProto : scenarioAdded.getActionList()) {
                    DeviceAction action = new DeviceAction();
                    action.setSensorId(actionProto.getSensorId());
                    action.setType(actionProto.getType().name());
                    action.setValue(actionProto.hasValue() ? actionProto.getValue() : null);
                    actions.add(action);
                }

                ScenarioAddedEvent scenarioAddedEvent = new ScenarioAddedEvent();
                scenarioAddedEvent.setName(scenarioAdded.getName());
                scenarioAddedEvent.setConditions(conditions);
                scenarioAddedEvent.setActions(actions);
                result = scenarioAddedEvent;
                break;

            case SCENARIO_REMOVED:
                ScenarioRemovedEventProto scenarioRemoved = proto.getScenarioRemoved();

                ScenarioRemovedEvent scenarioRemovedEvent = new ScenarioRemovedEvent();
                scenarioRemovedEvent.setName(scenarioRemoved.getName());
                result = scenarioRemovedEvent;
                break;

            default:
                throw new IllegalArgumentException(
                        "Тип hub нераспознан: " + proto.getPayloadCase());
        }

        result.setHubId(proto.getHubId());
        result.setTimestamp(toInstant(proto.getTimestamp()));

        return result;
    }

    private static Instant toInstant(com.google.protobuf.Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    private static Object mapConditionValue(ScenarioConditionProto condition) {
        return switch (condition.getValueCase()) {
            case BOOL_VALUE -> condition.getBoolValue();
            case INT_VALUE -> condition.getIntValue();
            case VALUE_NOT_SET -> null;
        };
    }
}
