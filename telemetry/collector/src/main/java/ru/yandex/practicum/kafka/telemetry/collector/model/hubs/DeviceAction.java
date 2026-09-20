package ru.yandex.practicum.kafka.telemetry.collector.model.hubs;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DeviceAction {
    private String sensorId;
    private String type;
    private Integer value;
}