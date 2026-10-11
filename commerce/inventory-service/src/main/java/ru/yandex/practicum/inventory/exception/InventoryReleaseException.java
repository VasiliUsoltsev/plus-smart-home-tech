package ru.yandex.practicum.inventory.exception;

public class InventoryReleaseException extends RuntimeException {
    public InventoryReleaseException(String message) {
        super(message);
    }
}
