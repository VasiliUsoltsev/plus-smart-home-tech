package ru.yandex.practicum.product.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.service.ProductService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/api/products")
public class ProductController {
    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @PatchMapping("/{id}")
    public ProductDto update(@Valid @RequestBody UpdateProductRequest request,
                             @PathVariable @Positive Long id
    ) {
        return productService.update(request, id);
    }

    @GetMapping("/{id}")
    public ProductDto get(@PathVariable @Positive Long id) {
        return productService.get(id);
    }

    @GetMapping
    public List<ProductDto> getAll() {
        return productService.getAll();
    }

    @GetMapping("/search")
    public List<ProductDto> search(@RequestParam String query) {
        return productService.search(query);
    }

    @GetMapping("/category/{categoryId}")
    List<ProductDto> getByCategoryId(@PathVariable @Positive Long categoryId) {
        return productService.getByCategoryId(categoryId);
    }
}
