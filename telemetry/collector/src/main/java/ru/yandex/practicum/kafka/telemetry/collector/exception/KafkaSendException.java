package ru.yandex.practicum.kafka.telemetry.collector.exception;

public class KafkaSendException extends RuntimeException {
    public KafkaSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
