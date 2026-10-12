package ru.yandex.practicum.order.client.fallbackfactory;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.client.ProductClient;
import ru.yandex.practicum.order.dto.client.ProductDto;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;

@Slf4j
@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {
    @Override
    public ProductClient create(Throwable cause) {
        return new ProductClient() {
            @Override
            public ProductDto getProductById(Long id) {
                if (cause instanceof FeignException exception && (exception.status() / 100) == 4) {
                    log.warn(
                            "Бизнес отказ от product-service: {}",
                            exception.status()
                    );

                    throw exception;
                }

                log.warn(
                        "product-service недоступен при запросе товара id={}",
                        id,
                        cause
                );

                throw new ProductServiceUnavailableException(id, cause);
            }
        };
    }
}
