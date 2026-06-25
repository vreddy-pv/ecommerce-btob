package com.btob.order.mcp;

import com.btob.order.dto.CreateOrderRequest;
import com.btob.order.dto.OrderResponse;
import com.btob.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MCP Tools for order management.
 * Exposes order operations to AI agents via MCP protocol.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderMcpTools {

    private final OrderService orderService;

    /**
     * Check the status of an order by ID.
     *
     * @param orderId The order ID to check
     * @return Map with order status, total amount, item count, and creation date
     */
    @Tool(description = "Check the status of a B2B order by order ID. Returns order status (PENDING, CONFIRMED, SHIPPED, DELIVERED), total amount, item count, and creation date.")
    public Map<String, Object> check_order_status(
            @ToolParam(description = "The order ID to check (UUID format)") String orderId) {

        log.info("MCP Tool called: check_order_status({})", orderId);

        try {
            OrderResponse order = orderService.getOrder(UUID.fromString(orderId));

            return Map.of(
                    "orderId", order.getId().toString(),
                    "status", order.getStatus().name(),
                    "totalAmount", order.getTotalAmount(),
                    "itemCount", order.getItems().size(),
                    "createdAt", order.getCreatedAt().toString()
            );
        } catch (Exception e) {
            log.error("Error checking order status", e);
            return Map.of(
                    "orderId", orderId,
                    "status", "ERROR",
                    "message", "Failed to check order status: " + e.getMessage()
            );
        }
    }

    /**
     * List orders for an account.
     *
     * @param accountId The account ID to list orders for
     * @param status Optional status filter (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
     * @return List of orders with ID, status, total amount, and item count
     */
    @Tool(description = "List recent orders for an account. Returns order IDs, statuses, and totals. Use this when the user provides a partial order ID or wants to see their orders.")
    public List<Map<String, Object>> list_orders(
            @ToolParam(description = "The account ID to list orders for (UUID format)") String accountId,
            @ToolParam(description = "Optional status filter: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED", required = false) String status) {

        log.info("MCP Tool called: list_orders(accountId={}, status={})", accountId, status);

        try {
            UUID accountUuid = UUID.fromString(accountId);
            Page<OrderResponse> orders;

            if (status != null && !status.isBlank()) {
                var orderStatus = com.btob.order.entity.OrderStatus.valueOf(status.toUpperCase());
                orders = orderService.getOrdersByAccountAndStatus(accountUuid, orderStatus, 0, 50);
            } else {
                orders = orderService.getOrdersByAccount(accountUuid, 0, 50);
            }

            return orders.getContent().stream()
                    .map(order -> Map.<String, Object>of(
                            "orderId", order.getId().toString(),
                            "status", order.getStatus().name(),
                            "totalAmount", order.getTotalAmount(),
                            "itemCount", order.getItems().size(),
                            "createdAt", order.getCreatedAt().toString()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error listing orders", e);
            return List.of(Map.of(
                    "status", "ERROR",
                    "message", "Failed to list orders: " + e.getMessage()
            ));
        }
    }

    /**
     * Create a new B2B order with line items.
     *
     * @param accountId The account ID placing the order
     * @param items List of items with SKU and quantity
     * @return Map with order ID, status, total amount, and success message
     */
    @Tool(description = "Create a new B2B order with line items. Requires account ID and list of items (SKU + quantity). Returns order ID, status, and total amount. Full autonomy: no human approval required.")
    public Map<String, Object> create_b2b_order(
            @ToolParam(description = "The account ID placing the order (UUID format)") String accountId,
            @ToolParam(description = "List of order items, each with SKU and quantity") List<Map<String, Object>> items) {

        log.info("MCP Tool called: create_b2b_order(accountId={}, items={})", accountId, items.size());

        try {
            // Convert items to CreateOrderRequest.OrderItemRequest
            List<CreateOrderRequest.OrderItemRequest> orderItems = items.stream()
                    .map(item -> CreateOrderRequest.OrderItemRequest.builder()
                            .productSku((String) item.get("sku"))
                            .quantity((Integer) item.get("quantity"))
                            .build())
                    .toList();

            CreateOrderRequest request = CreateOrderRequest.builder()
                    .accountId(UUID.fromString(accountId))
                    .items(orderItems)
                    .build();

            OrderResponse order = orderService.createOrder(request);

            return Map.of(
                    "orderId", order.getId().toString(),
                    "status", order.getStatus().name(),
                    "totalAmount", order.getTotalAmount(),
                    "message", "Order created successfully"
            );
        } catch (Exception e) {
            log.error("Error creating order", e);
            return Map.of(
                    "status", "ERROR",
                    "message", "Failed to create order: " + e.getMessage()
            );
        }
    }
}
