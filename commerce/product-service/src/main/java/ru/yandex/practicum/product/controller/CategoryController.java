package ru.yandex.practicum.product.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.service.CategoryService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.create(request);
    }

    @GetMapping("/{id}")
    public CategoryDto get(@PathVariable @Positive Long id) {
        return categoryService.get(id);
    }

    @GetMapping
    public List<CategoryDto> getAll() {
        return categoryService.getAll();
    }
}
