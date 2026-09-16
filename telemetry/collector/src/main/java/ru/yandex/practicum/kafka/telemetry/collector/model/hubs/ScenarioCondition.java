package ru.yandex.practicum.kafka.telemetry.collector.model.hubs;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ScenarioCondition {
    private String sensorId;
    private String type;
    private String operation;
    private Integer value;
}