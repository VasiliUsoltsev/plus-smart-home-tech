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
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.service.OrderOrchestrationService;
import ru.yandex.practicum.order.service.OrderService;
import ru.yandex.practicum.order.service.ServiceCallResult;
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

        boolean degraded = false;

        for (OrderItem item : items) {
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();

            ProductDto product = null;

            if (products.containsKey(productId)) {
                product = products.get(productId);
            } else {
                ServiceCallResult<ProductDto> productResult = getProduct(productId);

                if (productResult instanceof ServiceCallResult.Failure<ProductDto> failure) {
                    throw new OrderProcessingException(failure.message());
                }

                if (productResult instanceof ServiceCallResult.Degraded<ProductDto>) {
                    degraded = true;
                    item.setProductName("Товар id=%d (ожидает проверки)".formatted(productId));
                    item.setPrice(BigDecimal.ZERO);
                    continue;
                }

                product = ((ServiceCallResult.Success<ProductDto>) productResult).value();
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
            ServiceCallResult<Void> reserveResult = reserve(entry.getKey(), entry.getValue());

            if (reserveResult instanceof ServiceCallResult.Failure<Void> failure) {
                release(succesReserves);
                throw new OrderProcessingException(failure.message());
            }

            if (reserveResult instanceof ServiceCallResult.Degraded<Void>) {
                degraded = true;
                continue;
            }

            succesReserves.put(entry.getKey(), entry.getValue());
        }

        order.setTotalPrice(totalPrice);

        if (degraded) {
            order.setStatus(OrderStatus.PENDING_CONFIRMATION);
            order.setStatusDetails("Заказ требует ручной проверки: часть данных недоступна");
        } else {
            order.setStatus(OrderStatus.CONFIRMED);
        }

        return orderService.saveOrder(order);
    }

    private ServiceCallResult<ProductDto> getProduct(Long productId) {
        ProductDto product = null;
        try {
            product = productClient.getProductById(productId);
        } catch (ProductServiceUnavailableException e) {
            return new ServiceCallResult.Degraded<>("Не удалось получить данные товара");
        } catch (FeignException e) {
            return mapProductException(e, productId);
        }

        if (product == null) {
            return new ServiceCallResult.Failure<>("Товар с id=%d не найден".formatted(productId));
        }

        if (!Boolean.TRUE.equals(product.active())) {
            return new ServiceCallResult.Failure<>("Товар с id=%d найден, но неактивен".formatted(productId));
        }

        return new ServiceCallResult.Success<>(product);
    }

    private ServiceCallResult<Void> reserve(Long productId, Integer quantity) {
        try {
            ReserveRequest reserveRequest = new ReserveRequest(productId, quantity);
            inventoryClient.reserveStock(reserveRequest);
            return new ServiceCallResult.Success<>(null);
        } catch (InventoryServiceUnavailableException e) {
            return new ServiceCallResult.Degraded<>("Не удалось зарезервировать товар id=%d".formatted(productId));
        } catch (FeignException e) {
            return mapReserveException(e, productId);
        }
    }

    private void release(Map<Long, Integer> succesReserves) {
        for (Map.Entry<Long, Integer> entry : succesReserves.entrySet()) {
            try {
                ReserveRequest request = new ReserveRequest(entry.getKey(), entry.getValue());
                inventoryClient.releaseStock(request);
            } catch (InventoryServiceUnavailableException e) {
                log.error("Не удалось компенсировать зарезервированный товар %d".formatted(entry.getKey()), e);
            } catch (FeignException e) {
                log.error("Не удалось компенсировать зарезервированный товар %d".formatted(entry.getKey()), e);
            }
        }
    }

    private ServiceCallResult<ProductDto> mapProductException(FeignException exception,
                                                              Long productId) {
        if (exception.status() == 404) {
            return new ServiceCallResult.Failure<>(
                    "Товар с id=%d не найден".formatted(productId)
            );
        }

        if (exception.status() / 100 == 4) {
            return new ServiceCallResult.Failure<>(
                    "Не удалось получить данные товара"
            );
        }

        return new ServiceCallResult.Degraded<>("Не удалось получить данные товара");
    }

    private ServiceCallResult<Void> mapReserveException(FeignException exception,
                                                        Long productId) {
        String message;
        if (exception.status() == 404) {
            message = "Складская запись для товара id=%d не найдена".formatted(productId);
            log.warn(message);
            return new ServiceCallResult.Failure<>(message);
        }

        if (exception.status() == 409) {
            message = "Недостаточно товара id=%d на складе".formatted(productId);
            log.warn(message);
            return new ServiceCallResult.Failure<>(message);
        }

        if (exception.status() / 100 == 4) {
            message = "Не удалось зарезервировать товар id=%d".formatted(productId);
            log.warn(message);
            return new ServiceCallResult.Failure<>(message);
        }

        message = "Не удалось зарезервировать товар id=%d".formatted(productId);
        log.error(message);
        return new ServiceCallResult.Degraded<>(message);
    }
}