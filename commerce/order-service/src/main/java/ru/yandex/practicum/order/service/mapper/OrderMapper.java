package ru.yandex.practicum.order.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemDto;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrderMapper {
    public static Order mapToOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .build();

        request.items()
                .stream()
                .map(OrderItemMapper::mapToOrderItem)
                .forEach(order::addItem);

        return order;
    }

    public static OrderDto mapToOrderDto (Order order) {
        List<OrderItemDto> orderItemDtos = order.getItems().stream()
                .map(OrderItemMapper::mapToOrderItemDto)
                .toList();

        return new OrderDto(order.getId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getStatus().toString(),
                order.getTotalPrice(),
                order.getStatusDetails(),
                order.getCreated(),
                orderItemDtos
        );
    }
}
