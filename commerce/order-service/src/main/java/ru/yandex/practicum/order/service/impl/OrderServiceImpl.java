package ru.yandex.practicum.order.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.entity.OrderStatus;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.repository.OrderItemRepository;
import ru.yandex.practicum.order.repository.OrderRepository;
import ru.yandex.practicum.order.service.OrderService;
import ru.yandex.practicum.order.service.mapper.OrderMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {
    private static final String ORDER_NOT_FOUND_EXCEPTION = "Заказ не найден, id=";

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderDto saveOrder(Order order) {
        order = orderRepository.save(order);

        return OrderMapper.mapToOrderDto(order);
    }

    @Override
    public OrderDto get(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ORDER_NOT_FOUND_EXCEPTION + id));

        return OrderMapper.mapToOrderDto(order);
    }

    @Override
    public List<OrderDto> getAll() {
        return orderRepository.findAll().stream()
                .map(OrderMapper::mapToOrderDto)
                .toList();
    }

    @Override
    public List<OrderDto> getByCustomerEmail(String email) {
        return orderRepository.findAllByCustomerEmail(email).stream()
                .map(OrderMapper::mapToOrderDto)
                .toList();
    }
}
