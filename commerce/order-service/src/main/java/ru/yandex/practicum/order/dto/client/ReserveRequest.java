package ru.yandex.practicum.order.dto.client;

public record ReserveRequest(
        Long productId,
        Integer quantity
) {
}
