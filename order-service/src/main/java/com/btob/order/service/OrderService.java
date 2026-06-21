package com.btob.order.service;

import com.btob.order.dto.CreateOrderRequest;
import com.btob.order.dto.OrderItemDto;
import com.btob.order.dto.OrderResponse;
import com.btob.order.entity.Order;
import com.btob.order.entity.OrderItem;
import com.btob.order.entity.OrderStatus;
import com.btob.order.event.OrderCreatedEvent;
import com.btob.order.event.OrderCreatedEvent.OrderItemEvent;
import com.btob.order.event.OrderStatusChangedEvent;
import com.btob.order.exception.InsufficientInventoryException;
import com.btob.order.exception.InvalidOrderStatusException;
import com.btob.order.exception.ResourceNotFoundException;
import com.btob.order.repository.OrderItemRepository;
import com.btob.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Order business logic with status management.
 * Satisfies ORDER-01 (create with line items), ORDER-02 (status tracking), ORDER-03 (order history).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderEventPublisher orderEventPublisher;

    // Mock product data for validation (in real app, this would call catalog-service)
    private static final Map<String, BigDecimal> PRODUCT_PRICES;
    private static final Map<String, Integer> PRODUCT_INVENTORY;

    static {
        Map<String, BigDecimal> prices = new java.util.HashMap<>();
        prices.put("BRK-001", new BigDecimal("29.99"));
        prices.put("BRK-002", new BigDecimal("49.99"));
        prices.put("BRK-003", new BigDecimal("65.50"));
        prices.put("BRK-004", new BigDecimal("125.00"));
        prices.put("BRK-005", new BigDecimal("12.99"));
        prices.put("ELC-001", new BigDecimal("89.99"));
        prices.put("ELC-002", new BigDecimal("149.99"));
        prices.put("ELC-003", new BigDecimal("79.99"));
        prices.put("ELC-004", new BigDecimal("34.99"));
        prices.put("ELC-005", new BigDecimal("9.99"));
        prices.put("ENG-001", new BigDecimal("19.99"));
        prices.put("ENG-002", new BigDecimal("24.99"));
        prices.put("ENG-003", new BigDecimal("14.99"));
        prices.put("ENG-004", new BigDecimal("15.99"));
        prices.put("ENG-005", new BigDecimal("39.99"));
        prices.put("FLT-001", new BigDecimal("19.99"));
        prices.put("FLT-002", new BigDecimal("14.99"));
        prices.put("SUS-001", new BigDecimal("79.99"));
        prices.put("SUS-002", new BigDecimal("119.99"));
        prices.put("SUS-003", new BigDecimal("159.99"));
        PRODUCT_PRICES = java.util.Collections.unmodifiableMap(prices);

        Map<String, Integer> inventory = new java.util.HashMap<>();
        inventory.put("BRK-001", 100);
        inventory.put("BRK-002", 75);
        inventory.put("BRK-003", 80);
        inventory.put("BRK-004", 45);
        inventory.put("BRK-005", 200);
        inventory.put("ELC-001", 60);
        inventory.put("ELC-002", 40);
        inventory.put("ELC-003", 55);
        inventory.put("ELC-004", 120);
        inventory.put("ELC-005", 300);
        inventory.put("ENG-001", 250);
        inventory.put("ENG-002", 180);
        inventory.put("ENG-003", 150);
        inventory.put("ENG-004", 400);
        inventory.put("ENG-005", 90);
        inventory.put("FLT-001", 350);
        inventory.put("FLT-002", 280);
        inventory.put("SUS-001", 60);
        inventory.put("SUS-002", 40);
        inventory.put("SUS-003", 35);
        PRODUCT_INVENTORY = java.util.Collections.unmodifiableMap(inventory);
    }

    /**
     * Create a new order with line items.
     * Per ORDER-01: Create order with line items (SKU + quantity).
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for account: {}", request.getAccountId());

        // Validate products exist and have sufficient inventory
        validateInventory(request.getItems());

        // Calculate total amount from line items
        BigDecimal totalAmount = BigDecimal.ZERO;

        // Create Order entity with PENDING status
        Order order = Order.builder()
                .accountId(request.getAccountId())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .creditUsed(BigDecimal.ZERO)
                .orderItems(new ArrayList<>())
                .build();

        // Create OrderItem entities for each line item
        List<OrderCreatedEvent.OrderItemEvent> eventItems = new ArrayList<>();
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            BigDecimal unitPrice = PRODUCT_PRICES.get(itemRequest.getProductSku());
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem item = OrderItem.builder()
                    .productSku(itemRequest.getProductSku())
                    .productName(getProductName(itemRequest.getProductSku()))
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(itemTotal)
                    .build();

            order.addOrderItem(item);
            totalAmount = totalAmount.add(itemTotal);

            eventItems.add(OrderCreatedEvent.OrderItemEvent.builder()
                    .productSku(itemRequest.getProductSku())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .build());
        }

        order.setTotalAmount(totalAmount);

        // Save order and items
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());

        // Publish OrderCreatedEvent
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .accountId(savedOrder.getAccountId())
                .totalAmount(savedOrder.getTotalAmount())
                .items(eventItems)
                .createdAt(savedOrder.getCreatedAt())
                .build();
        orderEventPublisher.publishOrderCreated(event);

        // Return OrderResponse
        return mapToResponse(savedOrder);
    }

    /**
     * Get order by ID with all line items.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    /**
     * Get paginated order history for an account.
     * Per ORDER-03: View order history.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByAccount(UUID accountId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageRequest)
                .map(this::mapToResponse);
    }

    /**
     * Update order status with valid transition validation.
     * Per ORDER-02: Status tracking with PENDING -> CONFIRMED -> SHIPPED -> DELIVERED.
     */
    @Transactional
    public OrderResponse updateOrderStatus(UUID id, OrderStatus newStatus) {
        log.info("Updating order {} status to {}", id, newStatus);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        OrderStatus oldStatus = order.getStatus();

        // Validate status transition
        validateStatusTransition(oldStatus, newStatus);

        // Update status
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        // Publish OrderStatusChangedEvent
        OrderStatusChangedEvent event = OrderStatusChangedEvent.builder()
                .orderId(updatedOrder.getId())
                .accountId(updatedOrder.getAccountId())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .updatedAt(updatedOrder.getUpdatedAt())
                .build();
        orderEventPublisher.publishOrderStatusChanged(event);

        return mapToResponse(updatedOrder);
    }

    /**
     * Get order status only (for chatbot queries per CHAT-01).
     */
    @Transactional(readOnly = true)
    public OrderStatus getOrderStatus(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return order.getStatus();
    }

    /**
     * Validate that all products exist and have sufficient inventory.
     */
    private void validateInventory(List<CreateOrderRequest.OrderItemRequest> items) {
        for (CreateOrderRequest.OrderItemRequest item : items) {
            if (!PRODUCT_PRICES.containsKey(item.getProductSku())) {
                throw new ResourceNotFoundException("Product not found with SKU: " + item.getProductSku());
            }
            Integer available = PRODUCT_INVENTORY.get(item.getProductSku());
            if (available == null || available < item.getQuantity()) {
                throw new InsufficientInventoryException(
                        "Insufficient inventory for SKU: " + item.getProductSku() +
                                ". Available: " + available + ", Requested: " + item.getQuantity());
            }
        }
    }

    /**
     * Validate status transition is allowed.
     * PENDING -> CONFIRMED -> SHIPPED -> DELIVERED
     */
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED;
            case CONFIRMED -> next == OrderStatus.SHIPPED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            case DELIVERED -> false;
        };

        if (!valid) {
            throw new InvalidOrderStatusException(
                    "Invalid status transition from " + current + " to " + next);
        }
    }

    /**
     * Get product name from SKU (mock data matching catalog).
     */
    private String getProductName(String sku) {
        return switch (sku) {
            case "BRK-001" -> "Front Brake Pads";
            case "BRK-002" -> "Rear Brake Pads";
            case "BRK-003" -> "Brake Rotors";
            case "BRK-004" -> "Brake Caliper";
            case "BRK-005" -> "Brake Fluid";
            case "ELC-001" -> "Car Battery";
            case "ELC-002" -> "Alternator";
            case "ELC-003" -> "Starter Motor";
            case "ELC-004" -> "Headlights";
            case "ELC-005" -> "Fuses";
            case "ENG-001" -> "Spark Plugs";
            case "ENG-002" -> "Engine Oil";
            case "ENG-003" -> "Oil Filter";
            case "ENG-004" -> "Air Filter";
            case "ENG-005" -> "Timing Belt";
            case "FLT-001" -> "Cabin Air Filter";
            case "FLT-002" -> "Fuel Filter";
            case "SUS-001" -> "Shock Absorbers";
            case "SUS-002" -> "Struts";
            case "SUS-003" -> "Control Arms";
            default -> "Unknown Product";
        };
    }

    /**
     * Map Order entity to OrderResponse DTO.
     */
    private OrderResponse mapToResponse(Order order) {
        List<OrderItemDto> itemDtos = order.getOrderItems().stream()
                .map(item -> OrderItemDto.builder()
                        .id(item.getId())
                        .productSku(item.getProductSku())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .accountId(order.getAccountId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .creditUsed(order.getCreditUsed())
                .items(itemDtos)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
