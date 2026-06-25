package com.btob.order.service;

import com.btob.order.dto.CreateOrderRequest;
import com.btob.order.dto.OrderItemDto;
import com.btob.order.dto.OrderResponse;
import com.btob.order.entity.Order;
import com.btob.order.entity.OrderItem;
import com.btob.order.entity.OrderStatus;
import com.btob.order.event.OrderCancelledEvent;
import com.btob.order.event.OrderCreatedEvent;
import com.btob.order.event.OrderCreatedEvent.OrderItemEvent;
import com.btob.order.event.OrderStatusChangedEvent;
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
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final RestTemplate restTemplate;

    private static final String CATALOG_SERVICE_URL = "http://localhost:8082/api/catalog/products";

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for account: {}", request.getAccountId());

        BigDecimal totalAmount = BigDecimal.ZERO;

        Order order = Order.builder()
                .accountId(request.getAccountId())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .creditUsed(BigDecimal.ZERO)
                .orderItems(new ArrayList<>())
                .build();

        List<OrderItemEvent> eventItems = new ArrayList<>();
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Map<String, Object> product = fetchProduct(itemRequest.getProductSku());
            BigDecimal unitPrice = new BigDecimal(product.get("basePrice").toString());

            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem item = OrderItem.builder()
                    .productSku(itemRequest.getProductSku())
                    .productName((String) product.get("name"))
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(itemTotal)
                    .build();

            order.addOrderItem(item);
            totalAmount = totalAmount.add(itemTotal);

            eventItems.add(OrderItemEvent.builder()
                    .productSku(itemRequest.getProductSku())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .build());
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .accountId(savedOrder.getAccountId())
                .totalAmount(savedOrder.getTotalAmount())
                .items(eventItems)
                .createdAt(savedOrder.getCreatedAt())
                .build();
        orderEventPublisher.publishOrderCreated(event);

        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByAccount(UUID accountId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageRequest)
                .map(this::mapToResponse);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID id, OrderStatus newStatus) {
        log.info("Updating order {} status to {}", id, newStatus);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        OrderStatus oldStatus = order.getStatus();
        validateStatusTransition(oldStatus, newStatus);
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

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

    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        log.info("Cancelling order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOrderStatusException(
                    "Cannot cancel order in status: " + order.getStatus());
        }

        boolean wasConfirmed = order.getStatus() == OrderStatus.CONFIRMED;
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);

        List<OrderCancelledEvent.OrderItemEvent> cancelItems = cancelledOrder.getOrderItems().stream()
                .map(item -> OrderCancelledEvent.OrderItemEvent.builder()
                        .productSku(item.getProductSku())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(cancelledOrder.getId())
                .accountId(cancelledOrder.getAccountId())
                .items(cancelItems)
                .wasConfirmed(wasConfirmed)
                .reason("User requested cancellation")
                .timestamp(LocalDateTime.now())
                .build();
        orderEventPublisher.publishOrderCancelled(event);

        log.info("Order {} cancelled (was {}). Event published.", orderId, oldStatus);
        return mapToResponse(cancelledOrder);
    }

    @Transactional(readOnly = true)
    public OrderStatus getOrderStatus(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return order.getStatus();
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!valid) {
            throw new InvalidOrderStatusException(
                    "Invalid status transition from " + current + " to " + next);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchProduct(String sku) {
        try {
            return restTemplate.getForObject(CATALOG_SERVICE_URL + "/" + sku, Map.class);
        } catch (Exception e) {
            log.warn("Failed to fetch product {} from catalog-service, using fallback: {}", sku, e.getMessage());
            return getFallbackProduct(sku);
        }
    }

    private Map<String, Object> getFallbackProduct(String sku) {
        String name = switch (sku) {
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
        return Map.of("sku", sku, "name", name, "basePrice", new BigDecimal("29.99"));
    }

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
