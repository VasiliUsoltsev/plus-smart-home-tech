package ru.yandex.practicum.order.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.order.dto.OrderItemDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.OrderItem;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrderItemMapper {
    public static OrderItem mapToOrderItem(OrderItemRequest request) {
        return OrderItem.builder()
                .productId(request.productId())
                .productName(request.productName())
                .quantity(request.quantity())
                .price(request.price())
                .build();
    }

    public static OrderItemDto mapToOrderItemDto(OrderItem orderItem) {
        return new OrderItemDto(orderItem.getId(),
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getPrice()
        );
    }
}
