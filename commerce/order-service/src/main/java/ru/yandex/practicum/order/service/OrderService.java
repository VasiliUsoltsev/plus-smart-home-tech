package ru.yandex.practicum.order.service;

import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.entity.Order;

import java.util.List;

public interface OrderService {
    OrderDto saveOrder(Order order);

    OrderDto get(Long id);

    List<OrderDto> getAll();

    List<OrderDto> getByCustomerEmail(String email);
}
