package ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class DeviceAddedEvent extends HubEvent {
    @NotBlank(message = "Неуказан идентификатор добавляемого устройства")
    private String id;

    @NotBlank(message = "Неуказан тип добавляемого устройства")
    private String deviceType;

    @Override
    public HubEventType getType() {
        return HubEventType.DEVICE_ADDED;
    }
}