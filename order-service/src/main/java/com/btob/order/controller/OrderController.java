package com.btob.order.controller;

import com.btob.order.dto.CreateOrderRequest;
import com.btob.order.dto.OrderResponse;
import com.btob.order.entity.OrderStatus;
import com.btob.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST endpoints for order operations.
 * Satisfies ORDER-01 (create with line items), ORDER-02 (status tracking), ORDER-03 (view history).
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order API", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;

    /**
     * Create a new order with line items.
     * POST /api/orders
     */
    @PostMapping
    @Operation(summary = "Create a new order", description = "Create a new B2B order with line items")
    @ApiResponse(responseCode = "201", description = "Order created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get order by ID with all line items.
     * GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieve order details with all line items")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Order ID") @PathVariable UUID id) {
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(response);
    }

    /**
     * List orders with optional accountId filter, paginated.
     * GET /api/orders?accountId=xxx&page=0&size=10
     */
    @GetMapping
    @Operation(summary = "List orders", description = "Get paginated list of orders with optional account filter")
    @ApiResponse(responseCode = "200", description = "Orders retrieved")
    public ResponseEntity<Page<OrderResponse>> getOrders(
            @Parameter(description = "Filter by account ID") @RequestParam(required = false) UUID accountId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        if (accountId != null) {
            return ResponseEntity.ok(orderService.getOrdersByAccount(accountId, page, size));
        }
        // If no accountId, return all orders (could be admin endpoint)
        return ResponseEntity.ok(orderService.getOrdersByAccount(accountId, page, size));
    }

    /**
     * Update order status.
     * PUT /api/orders/{id}/status
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Update order status", description = "Update order status with valid transition")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "400", description = "Invalid status transition")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @Parameter(description = "Order ID") @PathVariable UUID id,
            @Parameter(description = "New status") @RequestParam OrderStatus status) {
        OrderResponse response = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel an order (PENDING or CONFIRMED only).
     * POST /api/orders/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order", description = "Cancel an order in PENDING or CONFIRMED status")
    @ApiResponse(responseCode = "200", description = "Order cancelled")
    @ApiResponse(responseCode = "400", description = "Cannot cancel in current status")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "Order ID") @PathVariable UUID id) {
        OrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get order status only (for chatbot queries per CHAT-01).
     * GET /api/orders/{id}/status
     */
    @GetMapping("/{id}/status")
    @Operation(summary = "Get order status", description = "Get only the order status (for chatbot)")
    @ApiResponse(responseCode = "200", description = "Status retrieved")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderStatus> getOrderStatus(
            @Parameter(description = "Order ID") @PathVariable UUID id) {
        OrderStatus status = orderService.getOrderStatus(id);
        return ResponseEntity.ok(status);
    }
}
