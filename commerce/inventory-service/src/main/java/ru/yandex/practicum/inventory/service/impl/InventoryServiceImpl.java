package ru.yandex.practicum.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.Inventory;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.InventoryAlreadyExistsException;
import ru.yandex.practicum.inventory.exception.NotFoundException;
import ru.yandex.practicum.inventory.repository.InventoryRepository;
import ru.yandex.practicum.inventory.service.InventoryService;
import ru.yandex.practicum.inventory.service.mapper.InventoryMapper;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {
    private static final String PRODUCT_NOT_FOUND_EXCEPTION = "Записи по продукту не найдены, id=";
    private static final String PRODUCT_CONFLICT_EXCEPTION = "Данные по продукту уже внесены, id=";

    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public InventoryDto create(ReserveRequest request) {
        Inventory newInventory = InventoryMapper.mapToInventory(request);

        if (inventoryRepository.existsByProductId(newInventory.getProductId())) {
            throw new InventoryAlreadyExistsException(PRODUCT_CONFLICT_EXCEPTION + newInventory.getProductId());
        }

        newInventory.setReserved(0);

        newInventory = inventoryRepository.save(newInventory);

        return InventoryMapper.mapToInventoryDto(newInventory);
    }

    @Override
    @Transactional
    public InventoryDto update(UpdateInventoryRequest request) {
        Inventory inventory = getProductById(request.productId());
        inventory.setQuantity(request.quantity());

        inventory = inventoryRepository.save(inventory);

        return InventoryMapper.mapToInventoryDto(inventory);
    }

    @Override
    public List<InventoryDto> getAll() {
        return inventoryRepository.findAll().stream()
                .map(InventoryMapper::mapToInventoryDto)
                .toList();
    }

    @Override
    public InventoryDto getByProductId(Long id) {
        Inventory inventory = inventoryRepository.findByProductId(id)
                .orElseThrow(() -> new NotFoundException(PRODUCT_NOT_FOUND_EXCEPTION + id));

        return InventoryMapper.mapToInventoryDto(inventory);
    }

    @Override
    @Transactional
    public ReserveResponse reserve(ReserveRequest request) {
        Inventory inventory = getProductById(request.productId());

        // Если товара не хватает на складе
        if (inventory.getAvailableQuantity() < request.quantity()) {
            throw new InsufficientStockException("На складе товара менее " + request.quantity());
        }
        int updateReserved = inventory.getReserved() + request.quantity();
        inventory.setReserved(updateReserved);

        inventory = inventoryRepository.save(inventory);

        return new ReserveResponse(true,
                inventory.getAvailableQuantity(),
                "Товар успешно зарезервирован"
        );
    }

    private Inventory getProductById(Long product_id) {
        Optional<Inventory> optionalInventory = inventoryRepository.findByProductId(product_id);

        if (optionalInventory.isEmpty()) {
            throw new NotFoundException(PRODUCT_NOT_FOUND_EXCEPTION + product_id);
        }

        return optionalInventory.get();
    }
}
