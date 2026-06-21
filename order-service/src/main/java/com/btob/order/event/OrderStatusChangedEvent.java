package com.btob.order.event;

import com.btob.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when an order status changes.
 * Per D-05: Event for order status update state change.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangedEvent {

    private UUID orderId;
    private UUID accountId;
    private OrderStatus oldStatus;
    private OrderStatus newStatus;
    private LocalDateTime updatedAt;
}
