package ru.yandex.practicum.order;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.order.client.InventoryClient;
import ru.yandex.practicum.order.client.ProductClient;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.dto.client.ProductDto;
import ru.yandex.practicum.order.dto.client.ReserveRequest;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderStatus;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.service.OrderService;
import ru.yandex.practicum.order.service.impl.OrderOrchestrationServiceImpl;
import ru.yandex.practicum.order.service.mapper.OrderMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderOrchestrationServiceImplTest {

    @Mock
    private ProductClient productClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderOrchestrationServiceImpl orchestrator;

    private CreateOrderRequest request;
    private ProductDto product1;
    private ProductDto product2;

    @BeforeEach
    void setUp() {
        product1 = new ProductDto(
                1L,
                "Лампа",
                "Умная лампа",
                BigDecimal.valueOf(1490.00),
                true
        );
        product2 = new ProductDto(
                2L,
                "Замок",
                "Умный замок",
                BigDecimal.valueOf(500.00),
                true
        );

        request = new CreateOrderRequest(
                "Иван Петров",
                "ivan@example.com",
                List.of(
                        new OrderItemRequest(1L, 2),
                        new OrderItemRequest(2L, 3)
                )
        );
    }

    @Test
    @DisplayName("Успешное создание заказа: два товара, оба резерва прошли")
    void create_happyPath_savesOrderWithCorrectTotalPrice() {
        when(productClient.getProductById(1L)).thenReturn(product1);
        when(productClient.getProductById(2L)).thenReturn(product2);
        when(orderService.saveOrder(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100L);
            return OrderMapper.mapToOrderDto(o);
        });

        orchestrator.create(request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).saveOrder(captor.capture());
        Order saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(saved.getTotalPrice())
                .isEqualByComparingTo(BigDecimal.valueOf(4480.00));
        assertThat(saved.getItems()).hasSize(2);

        assertThat(saved.getItems().get(0).getProductName()).isEqualTo("Лампа");
        assertThat(saved.getItems().get(0).getPrice())
                .isEqualByComparingTo(BigDecimal.valueOf(1490.00));
        assertThat(saved.getItems().get(0).getQuantity()).isEqualTo(2);

        assertThat(saved.getItems().get(1).getProductName()).isEqualTo("Замок");
        assertThat(saved.getItems().get(1).getPrice())
                .isEqualByComparingTo(BigDecimal.valueOf(500.00));
        assertThat(saved.getItems().get(1).getQuantity()).isEqualTo(3);

        verify(productClient).getProductById(1L);
        verify(productClient).getProductById(2L);
        verify(inventoryClient).reserveStock(new ReserveRequest(1L, 2));
        verify(inventoryClient).reserveStock(new ReserveRequest(2L, 3));
        verifyNoMoreInteractions(inventoryClient);
    }

    @Test
    @DisplayName("Product-service 404 - OrderProcessingException, резервов нет")
    void create_productNotFound_throwsAndDoesNotReserve() {
        when(productClient.getProductById(1L))
                .thenThrow(feignException(404, "ProductClient#getProductById(Long)"));

        assertThatThrownBy(() -> orchestrator.create(request))
                .isInstanceOf(OrderProcessingException.class)
                .hasMessageContaining("не найден");

        verifyNoInteractions(inventoryClient);
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("Product-service вернул null - OrderProcessingException")
    void create_productIsNull_throwsNotFound() {
        when(productClient.getProductById(1L)).thenReturn(null);

        assertThatThrownBy(() -> orchestrator.create(request))
                .isInstanceOf(OrderProcessingException.class)
                .hasMessageContaining("не найден");

        verifyNoInteractions(inventoryClient);
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("Неактивный товар - OrderProcessingException, резервов нет")
    void create_productInactive_throwsAndDoesNotReserve() {
        ProductDto inactive = new ProductDto(
                1L,
                "Лампа",
                "Умная лампа",
                BigDecimal.valueOf(1490.00),
                false
        );
        when(productClient.getProductById(1L)).thenReturn(inactive);

        assertThatThrownBy(() -> orchestrator.create(request))
                .isInstanceOf(OrderProcessingException.class)
                .hasMessageContaining("неактивен");

        verifyNoInteractions(inventoryClient);
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("Inventory 409 на втором резерве - release первого, заказ не сохраняется")
    void create_insufficientStock_releasesFirstReserveAndDoesNotSave() {
        when(productClient.getProductById(1L)).thenReturn(product1);
        when(productClient.getProductById(2L)).thenReturn(product2);

        when(inventoryClient.reserveStock(new ReserveRequest(1L, 2))).thenReturn(null);
        when(inventoryClient.reserveStock(new ReserveRequest(2L, 3)))
                .thenThrow(feignException(409, "InventoryClient#reserveStock"));

        assertThatThrownBy(() -> orchestrator.create(request))
                .isInstanceOf(OrderProcessingException.class)
                .hasMessageContaining("Недостаточно");

        verify(inventoryClient).releaseStock(new ReserveRequest(1L, 2));
        verify(orderService, never()).saveOrder(any());
    }

    @Test
    @DisplayName("Inventory 404 на втором резерве - release первого, заказ не сохраняется")
    void create_reserveFailsWith404_releasesFirstAndThrows() {
        when(productClient.getProductById(1L)).thenReturn(product1);
        when(productClient.getProductById(2L)).thenReturn(product2);

        when(inventoryClient.reserveStock(new ReserveRequest(1L, 2))).thenReturn(null);
        when(inventoryClient.reserveStock(new ReserveRequest(2L, 3)))
                .thenThrow(feignException(404, "InventoryClient#reserveStock"));

        assertThatThrownBy(() -> orchestrator.create(request))
                .isInstanceOf(OrderProcessingException.class)
                .hasMessageContaining("не найдена");

        verify(inventoryClient).releaseStock(new ReserveRequest(1L, 2));
        verify(orderService, never()).saveOrder(any());
    }

    @Test
    @DisplayName("Два item с одним productId - product-service один раз, резерв с суммой")
    void create_duplicateProductId_mergesQuantityAndCallsProductOnce() {
        request = new CreateOrderRequest(
                "Иван Петров",
                "ivan@example.com",
                List.of(
                        new OrderItemRequest(1L, 2),
                        new OrderItemRequest(1L, 3)
                )
        );

        when(productClient.getProductById(1L)).thenReturn(product1);
        when(orderService.saveOrder(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100L);
            return OrderMapper.mapToOrderDto(o);
        });

        orchestrator.create(request);

        verify(productClient, times(1)).getProductById(1L);
        verify(inventoryClient, times(1)).reserveStock(new ReserveRequest(1L, 5));
        verify(orderService).saveOrder(any());
    }

    @Test
    @DisplayName("releaseStock падает - оркестратор всё равно бросает OrderProcessingException")
    void create_releaseFails_stillThrows() {
        when(productClient.getProductById(1L)).thenReturn(product1);
        when(productClient.getProductById(2L)).thenReturn(product2);

        when(inventoryClient.reserveStock(new ReserveRequest(1L, 2))).thenReturn(null);
        when(inventoryClient.reserveStock(new ReserveRequest(2L, 3)))
                .thenThrow(feignException(409, "InventoryClient#reserveStock"));

        when(inventoryClient.releaseStock(new ReserveRequest(1L, 2)))
                .thenThrow(feignException(500, "InventoryClient#releaseStock"));

        assertThatThrownBy(() -> orchestrator.create(request))
                .isInstanceOf(OrderProcessingException.class);

        verify(inventoryClient).releaseStock(new ReserveRequest(1L, 2));
        verify(orderService, never()).saveOrder(any());
    }

    @Test
    @DisplayName("product-service недоступен - PENDING_CONFIRMATION, товар-заглушка")
    void create_productServiceUnavailable_savesAsPendingConfirmation() {
        when(productClient.getProductById(1L))
                .thenThrow(new ProductServiceUnavailableException(1L, new RuntimeException("timeout")));
        when(productClient.getProductById(2L))
                .thenThrow(new ProductServiceUnavailableException(2L, new RuntimeException("timeout")));
        when(orderService.saveOrder(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100L);
            return OrderMapper.mapToOrderDto(o);
        });

        orchestrator.create(request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).saveOrder(captor.capture());
        Order saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
        assertThat(saved.getStatusDetails()).contains("ручной проверки");
        assertThat(saved.getItems()).hasSize(2);
        assertThat(saved.getItems().get(0).getProductName())
                .isEqualTo("Товар id=1 (ожидает проверки)");
        assertThat(saved.getItems().get(0).getPrice())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getItems().get(1).getProductName())
                .isEqualTo("Товар id=2 (ожидает проверки)");
        assertThat(saved.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("inventory-service недоступен - PENDING_CONFIRMATION, без компенсации")
    void create_inventoryServiceUnavailable_savesAsPendingConfirmation() {
        when(productClient.getProductById(1L)).thenReturn(product1);
        when(productClient.getProductById(2L)).thenReturn(product2);
        when(inventoryClient.reserveStock(any(ReserveRequest.class)))
                .thenThrow(new InventoryServiceUnavailableException(1L, new RuntimeException("circuit open")));
        when(orderService.saveOrder(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100L);
            return OrderMapper.mapToOrderDto(o);
        });

        orchestrator.create(request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).saveOrder(captor.capture());
        Order saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
        assertThat(saved.getStatusDetails()).contains("ручной проверки");
        assertThat(saved.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(4480.00));

        verify(inventoryClient, never()).releaseStock(any());
    }

    @Test
    @DisplayName("Оба сервиса недоступны - PENDING_CONFIRMATION, без компенсации")
    void create_bothServicesUnavailable_savesAsPendingConfirmation() {
        when(productClient.getProductById(anyLong()))
                .thenThrow(new ProductServiceUnavailableException(1L, new RuntimeException("timeout")));
        when(orderService.saveOrder(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100L);
            return OrderMapper.mapToOrderDto(o);
        });

        orchestrator.create(request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).saveOrder(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
        verifyNoInteractions(inventoryClient);
    }

    private static FeignException feignException(int status, String methodKey) {
        return FeignException.errorStatus(methodKey,
                Response.builder()
                        .status(status)
                        .reason("test")
                        .request(Request.create(
                                Request.HttpMethod.POST,
                                "/",
                                Map.of(),
                                null,
                                null,
                                null))
                        .build());
    }
}