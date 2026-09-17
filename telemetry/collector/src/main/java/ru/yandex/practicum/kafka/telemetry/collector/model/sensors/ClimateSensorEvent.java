package ru.yandex.practicum.kafka.telemetry.collector.model.sensors;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class ClimateSensorEvent extends SensorEvent {
    @NotNull(message = "Неуказан уровень температуры по шкале Цельсия")
    private Integer temperatureC;

    @NotNull(message = "Не указана влажность")
    private Integer humidity;

    @NotNull(message = "Неуказан уровень CO2")
    private Integer co2Level;

    @Override
    public SensorEventType getType() {
        return SensorEventType.CLIMATE_SENSOR_EVENT;
    }
}