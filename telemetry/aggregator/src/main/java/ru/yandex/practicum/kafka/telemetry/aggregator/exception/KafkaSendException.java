package ru.yandex.practicum.kafka.telemetry.aggregator.exception;

public class KafkaSendException extends RuntimeException {
    public KafkaSendException(String meddage) {
        super(meddage);
    }
}
