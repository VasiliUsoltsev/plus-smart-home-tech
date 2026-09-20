package ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class ScenarioRemovedEvent extends HubEvent {
    @NotBlank(message = "Не указано название удаляемого сценария")
    @Size(min = 3, message = "Длина названия должна быть не меньше 3 символов")
    private String name;

    @Override
    public HubEventType getType() {
        return HubEventType.SCENARIO_REMOVED;
    }
}