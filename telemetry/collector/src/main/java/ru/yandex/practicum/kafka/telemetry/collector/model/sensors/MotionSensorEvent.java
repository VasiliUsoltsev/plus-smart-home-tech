package ru.yandex.practicum.kafka.telemetry.collector.model.sensors;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class MotionSensorEvent extends SensorEvent {
    @NotNull(message = "Не указано качество связи")
    private Integer linkQuality;

    @NotNull(message = "Не зафиксировано наличие или отсутствие движения")
    private Boolean motion;

    @NotNull(message = "Не указано напряжение")
    private Integer voltage;

    @Override
    public SensorEventType getType() {
        return SensorEventType.MOTION_SENSOR_EVENT;
    }
}