package com.btob.order.repository;

import com.btob.order.entity.Order;
import com.btob.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA Repository for Order entity.
 * Satisfies ORDER-03 (order history queries).
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Find all orders for an account.
     */
    List<Order> findByAccountId(UUID accountId);

    /**
     * Find orders for an account, paginated and sorted by creation date descending.
     */
    Page<Order> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

    /**
     * Find orders by status.
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Find orders for an account filtered by status.
     */
    List<Order> findByAccountIdAndStatus(UUID accountId, OrderStatus status);
}
