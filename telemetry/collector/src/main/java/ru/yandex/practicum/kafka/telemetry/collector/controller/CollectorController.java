package ru.yandex.practicum.kafka.telemetry.collector.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.kafka.telemetry.collector.model.hubs.events.HubEvent;
import ru.yandex.practicum.kafka.telemetry.collector.model.sensors.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.collector.service.CollectorService;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/events")
public class CollectorController {
    private final CollectorService collectorService;

    @PostMapping("/sensors")
    public void collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        collectorService.sendSensorEvent(event);
    }

    @PostMapping("/hubs")
    public void collectHubEvent(@Valid @RequestBody HubEvent event) {
        collectorService.sendHubEvent(event);
    }
}
