package com.btob.order.repository;

import com.btob.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA Repository for OrderItem entity.
 * Provides line item queries for orders.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /**
     * Find all line items for an order.
     */
    List<OrderItem> findByOrderId(UUID orderId);
}
