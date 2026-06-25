package com.btob.catalog.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when an admin adjusts inventory (restock or write-off).
 * Per INVENTORY-04: Inventory adjustment tracking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAdjustmentEvent {

    private String sku;
    private int previousLevel;
    private int newLevel;
    private int delta;
    private String adminId;
    private LocalDateTime timestamp;
}
