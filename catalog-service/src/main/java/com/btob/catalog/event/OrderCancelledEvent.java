package com.btob.catalog.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event received when an order is cancelled.
 * Per INVENTORY-02: Release reserved inventory on cancellation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent {

    private UUID orderId;
    private UUID accountId;
    private List<OrderItemEvent> items;
    private boolean wasConfirmed;
    private String reason;
    private LocalDateTime timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEvent {
        private String productSku;
        private Integer quantity;
    }
}
