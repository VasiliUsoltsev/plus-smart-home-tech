package ru.yandex.practicum.collector.serialization.deserializer;

import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

public class HubEventDeserializer extends BaseAvroDeserializer<HubEventAvro> {
        public HubEventDeserializer() {
            super(HubEventAvro.getClassSchema());
        }
}
