package ru.yandex.practicum.order.exception;

public class ProductServiceUnavailableException extends RuntimeException {
    public ProductServiceUnavailableException(Long productId, Throwable cause) {
        super("Сервис каталога недоступен! Определено в рамках запроса по товару id=%d".formatted(productId), cause);
    }
}
