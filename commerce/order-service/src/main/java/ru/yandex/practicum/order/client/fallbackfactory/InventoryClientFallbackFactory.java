package ru.yandex.practicum.order.client.fallbackfactory;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.client.InventoryClient;
import ru.yandex.practicum.order.dto.client.ReserveRequest;
import ru.yandex.practicum.order.dto.client.ReserveResponse;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;

@Slf4j
@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {
    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {
            @Override
            public ReserveResponse reserveStock(ReserveRequest request) {
                if (cause instanceof FeignException exception && (exception.status() / 100) == 4) {
                    log.warn(
                            "Бизнес-отказ от inventory-service при резервировании товара id={}, status={}",
                            request.productId(),
                            exception.status()
                    );
                    throw exception;
                }

                log.warn(
                        "inventory-service недоступен при резервировании товара id={}",
                        request.productId(),
                        cause
                );

                throw new InventoryServiceUnavailableException(request.productId(), cause);
            }

            @Override
            public ReserveResponse releaseStock(ReserveRequest request) {
                if (cause instanceof FeignException exception && (exception.status() / 100) == 4) {
                    log.warn(
                            "Бизнес-отказ от inventory-service при снятии резерва товара id={}, status={}",
                            request.productId(),
                            exception.status()
                    );
                    throw exception;
                }

                log.warn(
                        "inventory-service недоступен при снятии резерва товара id={}",
                        request.productId(),
                        cause
                );

                throw new InventoryServiceUnavailableException(request.productId(), cause);
            }
        };
    }
}