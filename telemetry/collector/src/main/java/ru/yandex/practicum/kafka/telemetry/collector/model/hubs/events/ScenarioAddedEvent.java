package ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.DeviceAction;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.ScenarioCondition;

import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
public class ScenarioAddedEvent extends HubEvent {
    @NotBlank(message = "Не указано название добавленного сценария")
    @Size(min = 3, message = "Длина названия должна быть не меньше 3 символов")
    private String name;

    @NotEmpty(message = "Неуказан список условий, которые связаны со сценарием")
    List<ScenarioCondition> conditions;

    @NotEmpty(message = "Неуказан список действий, которые долны быть выполнены в рамках сценария")
    List<DeviceAction> actions;

    @Override
    public HubEventType getType() {
        return HubEventType.SCENARIO_ADDED;
    }
}