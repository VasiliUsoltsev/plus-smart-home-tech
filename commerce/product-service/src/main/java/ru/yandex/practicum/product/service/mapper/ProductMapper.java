package ru.yandex.practicum.product.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.entity.Product;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProductMapper {
    public static Product mapToProduct(CreateProductRequest request, Category category) {
        return Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .category(category)
                .imageUrl(request.imageUrl())
                .build();
    }

    public static ProductDto mapToProductDto(Product product) {
        return new ProductDto(product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                CategoryMapper.mapToCategoryDto(product.getCategory()),
                product.getImageUrl(),
                product.getActive()
        );
    }
}
