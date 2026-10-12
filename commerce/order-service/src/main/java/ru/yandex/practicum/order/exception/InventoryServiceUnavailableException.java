package ru.yandex.practicum.order.exception;

public class InventoryServiceUnavailableException extends RuntimeException {
    public InventoryServiceUnavailableException(Long productId, Throwable cause) {
        super("Сервис склада недоступен! Определено в рамках запроса по товару id=%d".formatted(productId), cause);
    }
}
