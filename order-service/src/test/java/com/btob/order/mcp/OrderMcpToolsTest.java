package com.btob.order.mcp;

import com.btob.order.dto.CreateOrderRequest;
import com.btob.order.dto.OrderItemDto;
import com.btob.order.dto.OrderResponse;
import com.btob.order.entity.OrderStatus;
import com.btob.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderMcpToolsTest {

    @Mock
    private OrderService orderService;

    private OrderMcpTools orderMcpTools;

    @BeforeEach
    void setUp() {
        orderMcpTools = new OrderMcpTools(orderService);
    }

    @Test
    void checkOrderStatusReturnsOrderData() {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = OrderResponse.builder()
                .id(orderId)
                .accountId(UUID.randomUUID())
                .status(OrderStatus.SHIPPED)
                .totalAmount(new BigDecimal("149.95"))
                .items(List.of(
                        OrderItemDto.builder().productSku("BRK-001").quantity(2).build(),
                        OrderItemDto.builder().productSku("ELC-002").quantity(1).build()
                ))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderService.getOrder(orderId)).thenReturn(response);

        Map<String, Object> result = orderMcpTools.check_order_status(orderId.toString());

        assertThat(result)
                .containsEntry("orderId", orderId.toString())
                .containsEntry("status", "SHIPPED")
                .containsEntry("totalAmount", new BigDecimal("149.95"))
                .containsEntry("itemCount", 2);
        assertThat(result.get("createdAt")).isNotNull();
    }

    @Test
    void checkOrderStatusHandlesException() {
        UUID orderId = UUID.randomUUID();
        when(orderService.getOrder(orderId))
                .thenThrow(new RuntimeException("Order not found"));

        Map<String, Object> result = orderMcpTools.check_order_status(orderId.toString());

        assertThat(result)
                .containsEntry("orderId", orderId.toString())
                .containsEntry("status", "ERROR");
        assertThat((String) result.get("message")).contains("Failed to check order status");
    }

    @Test
    void createB2bOrderReturnsOrderData() {
        UUID accountId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        List<Map<String, Object>> items = List.of(
                Map.of("sku", "BRK-001", "quantity", 2),
                Map.of("sku", "ELC-001", "quantity", 1)
        );

        OrderResponse response = OrderResponse.builder()
                .id(orderId)
                .accountId(accountId)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("149.97"))
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        Map<String, Object> result = orderMcpTools.create_b2b_order(accountId.toString(), items);

        assertThat(result)
                .containsEntry("orderId", orderId.toString())
                .containsEntry("status", "PENDING")
                .containsEntry("totalAmount", new BigDecimal("149.97"))
                .containsEntry("message", "Order created successfully");
    }

    @Test
    void createB2bOrderHandlesException() {
        UUID accountId = UUID.randomUUID();
        List<Map<String, Object>> items = List.of(Map.of("sku", "INVALID", "quantity", 1));

        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new RuntimeException("Product not found"));

        Map<String, Object> result = orderMcpTools.create_b2b_order(accountId.toString(), items);

        assertThat(result)
                .containsEntry("status", "ERROR");
        assertThat((String) result.get("message")).contains("Failed to create order");
    }
}
