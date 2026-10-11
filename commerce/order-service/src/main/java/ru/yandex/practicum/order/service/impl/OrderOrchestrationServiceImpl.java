package ru.yandex.practicum.order.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.client.InventoryClient;
import ru.yandex.practicum.order.client.ProductClient;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.client.ProductDto;
import ru.yandex.practicum.order.dto.client.ReserveRequest;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.entity.OrderStatus;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.service.OrderOrchestrationService;
import ru.yandex.practicum.order.service.OrderService;
import ru.yandex.practicum.order.service.mapper.OrderMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderOrchestrationServiceImpl implements OrderOrchestrationService {
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final OrderService orderService;

    @Override
    public OrderDto create(CreateOrderRequest request) {
        Map<Long, ProductDto> products = new HashMap<>();
        Map<Long, Integer> reserves = new HashMap<>();
        Map<Long, Integer> succesReserves = new HashMap<>();

        Order order = OrderMapper.mapToOrder(request);

        List<OrderItem> items = order.getItems();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItem item : items) {
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();

            ProductDto product = null;

            if (products.containsKey(productId)) {
                product = products.get(productId);
            } else {
                product = getProduct(productId);
                products.put(productId, product);
            }

            if (reserves.containsKey(productId)) {
                Integer totalQuantityItem = reserves.get(productId) + quantity;
                reserves.put(productId, totalQuantityItem);
            } else {
                reserves.put(productId, quantity);
            }

            item.setProductName(product.name());
            item.setPrice(product.price());

            BigDecimal itemPrice = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalPrice = totalPrice.add(itemPrice);
        }
        for (Map.Entry<Long, Integer> entry : reserves.entrySet()) {
            reserve(entry, succesReserves);
        }

        order.setTotalPrice(totalPrice);

        order.setStatus(OrderStatus.CONFIRMED);

        return orderService.saveOrder(order);
    }

    private ProductDto getProduct(Long productId) {
        ProductDto product = null;
        try {
            product = productClient.getProductById(productId);
        } catch (FeignException e) {
            throw mapProductException(e, productId);
        }

        if (product == null) {
            throw new OrderProcessingException("Товар с id=%d не найден".formatted(productId));
        }

        if (!product.active()) {
            throw new OrderProcessingException("Товар с id=%d найден, но неактивен".formatted(productId));
        }

        return product;
    }

    private void reserve(Map.Entry<Long, Integer> entry, Map<Long, Integer> succesReserves) {
        try {
            ReserveRequest reserveRequest = new ReserveRequest(entry.getKey(), entry.getValue());
            inventoryClient.reserveStock(reserveRequest);
            succesReserves.put(entry.getKey(), entry.getValue());
        } catch (FeignException e) {
            release(succesReserves);
            throw mapReserveException(e, entry.getKey());
        }
    }

    private void release(Map<Long, Integer> succesReserves) {
        for (Map.Entry<Long, Integer> entry : succesReserves.entrySet()) {
            try {
                ReserveRequest request = new ReserveRequest(entry.getKey(), entry.getValue());
                inventoryClient.releaseStock(request);
            } catch (FeignException e) {
                throw mapReleaseException(e, entry.getKey());
            }
        }
    }

    private OrderProcessingException mapProductException(FeignException exception,
                                                         Long productId) {
        if (exception.status() == 404) {
            return new OrderProcessingException(
                    "Товар с id=%d не найден".formatted(productId)
            );
        }

        return new OrderProcessingException(
                "Не удалось получить данные товара"
        );
    }

    private OrderProcessingException mapReserveException(FeignException exception,
                                                         Long productId) {
        String message;
        if (exception.status() == 404) {
            message = "Складская запись для товара id=%d не найдена".formatted(productId);
            log.warn(message);
            return new OrderProcessingException(message);
        }

        if (exception.status() == 409) {
            message = "Недостаточно товара id=%d на складе".formatted(productId);
            log.warn(message);
            return new OrderProcessingException(message);
        }

        message = "Не удалось зарезервировать товар id=%d".formatted(productId);
        log.error(message);
        return new OrderProcessingException(message);
    }

    private OrderProcessingException mapReleaseException(FeignException exception,
                                                         Long productId) {
        String message = "Не удалось компенсировать зарезервированный товар %d".formatted(productId);
        log.error(message);
        return new OrderProcessingException(message);
    }
}
