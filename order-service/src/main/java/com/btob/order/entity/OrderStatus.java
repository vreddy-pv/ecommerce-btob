package com.btob.order.entity;

/**
 * Order status states for tracking fulfillment.
 * Satisfies ORDER-02 (status tracking).
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED
}
