package ru.yandex.practicum.product.service;

import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;

import java.util.List;

public interface ProductService {
    ProductDto create(CreateProductRequest request);

    ProductDto update(UpdateProductRequest request, Long id);

    ProductDto get(Long id);

    List<ProductDto> getAll();

    List<ProductDto> search(String query);

    List<ProductDto> getByCategoryId(Long id);
}
