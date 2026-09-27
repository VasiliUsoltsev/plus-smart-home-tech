package ru.yandex.practicum.product.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.entity.Product;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.repository.CategoryRepository;
import ru.yandex.practicum.product.repository.ProductRepository;
import ru.yandex.practicum.product.service.ProductService;
import ru.yandex.practicum.product.service.mapper.ProductMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {
    private static final String CATEGORY_NOT_FOUND_EXCEPTION = "Категория не найдена, id=";
    private static final String PRODUCT_NOT_FOUND_EXCEPTION = "Продукт не найден, id=";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public ProductDto create(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException(CATEGORY_NOT_FOUND_EXCEPTION + request.categoryId()));

        Product product = ProductMapper.mapToProduct(request, category);
        product.setActive(true);

        product = productRepository.save(product);

        return ProductMapper.mapToProductDto(product);
    }

    @Override
    @Transactional
    public ProductDto update(UpdateProductRequest request, Long id) {
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException(CATEGORY_NOT_FOUND_EXCEPTION + request.categoryId()));
        }

        Product oldProduct = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(PRODUCT_NOT_FOUND_EXCEPTION + id));

        if (request.name() != null) {
            oldProduct.setName(request.name());
        }

        if (request.description() != null) {
            oldProduct.setDescription(request.description());
        }

        if (request.price() != null) {
            oldProduct.setPrice(request.price());
        }

        if (category != null) {
            oldProduct.setCategory(category);
        }

        if (request.imageUrl() != null) {
            oldProduct.setImageUrl(request.imageUrl());
        }

        if (request.active() != null) {
            oldProduct.setActive(request.active());
        }

        oldProduct = productRepository.save(oldProduct);

        return ProductMapper.mapToProductDto(oldProduct);
    }

    @Override
    public ProductDto get(Long id) {
        Product result = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(PRODUCT_NOT_FOUND_EXCEPTION + id));

        return ProductMapper.mapToProductDto(result);
    }

    @Override
    public List<ProductDto> getAll() {
        return productRepository.findAllByActiveTrue().stream()
                .map(ProductMapper::mapToProductDto)
                .toList();
    }

    @Override
    public List<ProductDto> search(String query) {
        return productRepository.findByNameContainingIgnoreCase(query).stream()
                .map(ProductMapper::mapToProductDto)
                .toList();
    }

    @Override
    public List<ProductDto> getByCategoryId(Long id) {
        return productRepository.findAllByCategoryId(id).stream()
                .map(ProductMapper::mapToProductDto)
                .toList();
    }
}
