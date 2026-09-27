package ru.yandex.practicum.inventory.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.entity.Inventory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InventoryMapper {
    public static Inventory mapToInventory(ReserveRequest request) {
        return Inventory.builder()
                .productId(request.productId())
                .quantity(request.quantity())
                .build();
    }

    public static InventoryDto mapToInventoryDto(Inventory inventory) {
        InventoryDto result = new InventoryDto(inventory.getId(),
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getReserved(),
                inventory.getAvailableQuantity()
        );

        return result;
    }
}
