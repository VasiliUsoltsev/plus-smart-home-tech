package ru.yandex.practicum.order.dto.client;

public record ReserveResponse(

        boolean success,

        Integer availableQuantity,

        String message
) {
}